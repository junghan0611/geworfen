#!/usr/bin/env bash
# agent-emacs.sh — agent emacs 데몬을 **전용 tmux 세션**에서 띄우고 관리한다.
#
# 왜 systemd 가 아니라 tmux 인가 (GLG 결정, 2026-09-02):
#   "systemd를 지금 안정권이 아닌 상황에선 비추야. 계속 재시작할까봐.
#    다 꺼져도 되는데 이걸 geworfen에서 이맥스 tmux로 띄우고 관리하게 해야된다."
#
#   정확히 하자면 `agent-emacs.service` 는 `Restart=no` 라 이미 재시작하지 않는다
#   (2026-07-01 watchdog 자해 루프 16,354회를 겪고 watchdog + Restart=always 를
#   둘 다 제거했다 — CHANGELOG [2026.7.1]). 그러니 "systemd = 재시작"은 우리
#   유닛에 대해선 사실이 아니다. 그럼에도 tmux 를 정본으로 삼는 이유는 따로 있다:
#     - 죽으면 **창에 그대로 남는다.** journalctl 을 열지 않아도 눈에 보인다.
#     - 재부팅 후 자동으로 살아나지 않는다 = 사람이 의도해야 뜬다.
#       "안 되면 화면 안 뜨면 된다"는 운영 방침과 같은 방향이다.
#     - 상태를 추론할 필요가 없다. `tmux attach` 하면 그 프로세스가 거기 있다.
#
# 왜 **전용** 세션인가:
#   2026-09-02 이관 중 데몬이 GLG 작업창(`doomemacs-config:1.1`)의 tmux scope 에
#   얹혔다. `KillMode=control-group` 이라 그 창을 닫으면 데몬이 같이 죽는데,
#   창을 쓰는 사람은 그 사실을 모른다. 수명을 사람의 작업과 분리하는 것이
#   이 스크립트의 핵심이다. `--fg-daemon` 을 쓰는 것도 같은 이유다 —
#   tmux 가 프로세스를 직접 쥐어야 죽음이 보인다(`--daemon` 은 fork 후 사라진다).
#
# 사용:
#   ./ops/tmux/agent-emacs.sh start     # 전용 세션에 데몬 기동 (idempotent)
#   ./ops/tmux/agent-emacs.sh status    # 지금 누가 소켓을 쥐고 있나
#   ./ops/tmux/agent-emacs.sh stop      # SIGTERM (소켓 파일까지 정리된다)
#   ./ops/tmux/agent-emacs.sh restart
#   ./ops/tmux/agent-emacs.sh attach    # 데몬 창 들여다보기
set -uo pipefail

SESSION="${AGENT_EMACS_TMUX_SESSION:-agent-emacs}"
SOCKET="${GEWORFEN_EMACS_SOCKET:-server}"
EMACS="${AGENT_EMACS_BIN:-/run/current-system/sw/bin/emacs}"
EMACSCLIENT="${AGENT_EMACSCLIENT_BIN:-/run/current-system/sw/bin/emacsclient}"
INIT_DIR="${AGENT_EMACS_INIT_DIR:-/tmp/agent-emacs-init}"
LOAD_FILE="${AGENT_EMACS_LOAD:-$HOME/.doom.d/bin/agent-server.el}"
SOCKET_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/emacs"

say() { printf '%s\n' "$*" >&2; }

# 소켓이 살아서 답하는가. 데몬 유무의 유일한 진실 — ps 나 systemd 상태가 아니다.
alive() { timeout 8 "$EMACSCLIENT" -s "$SOCKET" --eval '(+ 1 1)' >/dev/null 2>&1; }

server_version() {
	timeout 8 "$EMACSCLIENT" -s "$SOCKET" --eval '(emacs-version)' 2>/dev/null \
		| sed -n 's/.*GNU Emacs \([0-9][0-9.]*\).*/\1/p'
}

# 소켓을 쥔 프로세스. 유닛 밖에서 도는 고아를 찾아내는 경로이기도 하다.
#
# ⚠️ `pgrep -f` 는 **명령줄에 그 문자열이 들어간 아무 프로세스나** 잡는다 — 그걸
#    입력한 셸, 그 셸의 자식, 이 스크립트를 부른 명령까지. 2026-09-02에 실제로
#    `pgrep -f -- '--daemon=server'` 가 데몬 1개 + 이미 죽어가던 셸 자식 2개를
#    반환했다. 그대로 `kill -TERM $pids` 를 쳤으면 남의 프로세스를 죽였다.
#    (같은 함정을 그날 관측 두 번에서도 밟았다: `pgrep -f emacs-async-comp` 가
#     자기 자신을 세어 "native-comp 진행 중"으로 오독됐다.)
# 그래서 명령줄 매칭만으로 끝내지 않고 **실행 바이너리가 emacs 인지** 확인한다.
daemon_pids() {
	local p exe out=""
	for p in $(pgrep -u "$(id -u)" -f -- "--fg-daemon=$SOCKET|--daemon=$SOCKET" 2>/dev/null); do
		[ "$p" = "$$" ] && continue
		exe=$(readlink -f "/proc/$p/exe" 2>/dev/null) || continue
		case "$exe" in
			*/emacs|*/emacs-[0-9]*) out="$out $p" ;;
		esac
	done
	printf '%s\n' "${out# }"
}

# 이 PID 가 어느 tmux 세션에 속하나. **cgroup 으로는 알 수 없다** — tmux 가 만드는
# scope 이름이 `tmux-spawn-<uuid>.scope` 라 세션명이 안 들어간다(2026-09-02 실측).
# 그래서 tmux 에게 직접 묻는다. `--fg-daemon` + `exec` 로 띄우면 데몬이 곧 pane_pid 가
# 되고, `--daemon` 으로 fork 된 경우엔 같은 scope 안의 pane_pid 를 찾아 역추적한다.
tmux_owner() {
	local pid="$1" panes ppid sess cg other
	panes=$(tmux list-panes -a -F '#{pane_pid} #{session_name}:#{window_index}.#{pane_index}' 2>/dev/null) || return 0
	# 1) 직접 일치 (--fg-daemon + exec)
	while read -r ppid sess; do
		[ "$ppid" = "$pid" ] && { printf '%s' "$sess"; return 0; }
	done <<< "$panes"
	# 2) 같은 cgroup(scope) 안의 pane_pid 로 역추적 (--daemon 으로 fork 된 경우)
	cg=$(sed 's/.*://' "/proc/$pid/cgroup" 2>/dev/null | head -1)
	[ -n "$cg" ] || return 0
	while read -r ppid sess; do
		other=$(sed 's/.*://' "/proc/$ppid/cgroup" 2>/dev/null | head -1)
		[ "$other" = "$cg" ] && { printf '%s' "$sess"; return 0; }
	done <<< "$panes"
	return 0
}

warn_unit() {
	local state
	state=$(systemctl --user is-active agent-emacs.service 2>/dev/null || true)
	[ "$state" = active ] && say "⚠️  agent-emacs.service 가 active 다. tmux 로 또 띄우면 소켓이 충돌한다.
    먼저: systemctl --user stop agent-emacs.service"
	local file
	file=$(systemctl --user is-enabled agent-emacs.service 2>/dev/null || true)
	[ "$file" = enabled ] && say "⚠️  agent-emacs.service 가 enabled 다 — **재부팅하면 systemd 가 뜬다.**
    tmux 를 정본으로 쓸 거면: systemctl --user disable agent-emacs.service"
	return 0
}

cmd_status() {
	printf '세션      : '
	tmux has-session -t "$SESSION" 2>/dev/null && echo "$SESSION (있음)" || echo "$SESSION (없음)"
	printf '소켓      : '
	if alive; then echo "$SOCKET 응답함 — Emacs $(server_version)"; else echo "$SOCKET 무응답"; fi
	printf '프로세스  : '; daemon_pids | tr '\n' ' '; echo
	local pids p cg owner
	pids=$(daemon_pids)
	for p in $pids; do
		cg=$(sed 's/.*://' "/proc/$p/cgroup" 2>/dev/null | head -1)
		owner=$(tmux_owner "$p")
		printf '  PID %-8s tmux=%-24s cgroup=%s\n' "$p" "${owner:-—}" "$cg"
		case "$cg" in
			*agent-emacs.service*) say "  ℹ️  systemd 유닛이 소유 중." ;;
			*tmux-spawn*)
				if [ "${owner%%:*}" = "$SESSION" ]; then
					:   # 우리 전용 세션 — 정상
				else
					say "  ⚠️  이 데몬은 **다른 tmux 창**($owner)에 얹혀 있다.
      KillMode=control-group 이라 그 창을 닫으면 같이 죽는다. '$0 restart' 로 옮겨라."
				fi ;;
		esac
	done
	# 주의: is-active/is-enabled 는 상태를 stdout 에 쓰면서 비0 으로 끝난다.
	# `|| echo unknown` 을 붙이면 두 값이 같이 나와 출력이 깨진다.
	local ua ue
	ua=$(systemctl --user is-active agent-emacs.service 2>/dev/null || true)
	ue=$(systemctl --user is-enabled agent-emacs.service 2>/dev/null || true)
	printf '유닛      : %s / %s\n' "${ua:-unknown}" "${ue:-unknown}"
	printf '소켓 파일 : '; ls "$SOCKET_DIR" 2>/dev/null | tr '\n' ' '; echo
}

cmd_start() {
	if alive; then
		say "이미 살아 있다 — Emacs $(server_version). 중복 기동하지 않는다."
		say "다른 창에 얹혀 있는지 확인하려면: $0 status"
		return 0
	fi
	warn_unit
	[ -f "$LOAD_FILE" ] || { say "❌ load 파일이 없다: $LOAD_FILE"; return 1; }
	[ -x "$EMACS" ]     || { say "❌ emacs 가 없다: $EMACS"; return 1; }
	mkdir -p "$INIT_DIR"

	# 죽은 소켓 파일이 남아 있으면 기동이 막힌다(SIGKILL 로 죽은 흔적).
	if [ -S "$SOCKET_DIR/$SOCKET" ] && ! alive; then
		say "ℹ️  응답 없는 소켓 파일을 치운다: $SOCKET_DIR/$SOCKET"
		rm -f "$SOCKET_DIR/$SOCKET"
	fi

	tmux has-session -t "$SESSION" 2>/dev/null || tmux new-session -d -s "$SESSION" -n emacs
	# --fg-daemon: tmux 가 프로세스를 직접 쥔다. 죽으면 창에 남아 눈에 보인다.
	tmux send-keys -t "$SESSION:emacs" \
		"exec '$EMACS' --init-directory='$INIT_DIR' --fg-daemon='$SOCKET' --load '$LOAD_FILE'" C-m

	say "기동 요청함. 소켓 대기 중… (agent-server.el 로드에 콜드 ~7초)"
	local _i
	for _i in $(seq 1 60); do
		sleep 1
		if alive; then say "✅ 준비됨 — Emacs $(server_version) / 세션 $SESSION"; return 0; fi
	done
	say "❌ 60초 안에 소켓이 안 열렸다. 창을 직접 봐라: tmux attach -t $SESSION"
	return 1
}

cmd_stop() {
	local pids
	pids=$(daemon_pids)
	[ -z "$pids" ] && { say "도는 데몬이 없다."; return 0; }
	# SIGTERM 이어야 한다. SIGKILL 은 소켓 파일을 남겨 다음 기동을 막는다
	# (server.el 은 정상 종료 시 delete-file server-file 을 한다).
	say "SIGTERM → $pids"
	kill -TERM $pids 2>/dev/null
	local _i
	for _i in $(seq 1 20); do
		sleep 1
		alive || { say "✅ 내려갔다."; return 0; }
	done
	say "⚠️  20초 뒤에도 응답한다. 남은 PID: $(daemon_pids)"
	return 1
}

case "${1:-status}" in
	start)   cmd_start ;;
	stop)    cmd_stop ;;
	restart) cmd_stop; cmd_start ;;
	status)  cmd_status ;;
	attach)  tmux attach -t "$SESSION" ;;
	*) say "usage: $0 {start|stop|restart|status|attach}"; exit 2 ;;
esac
