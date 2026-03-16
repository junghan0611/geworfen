# geworfen 게보르펜

> 던져진 것들 — 한 인간의 존재 데이터를 가공 없이 드러내는 WebTUI 뷰어

기획을 던지는 자가 자기 자신의 던짐 속에 던져져 있다.  
— Heidegger

## 무엇인가

`www.junghanacs.com` = **시간과정신의방의 웹 뷰어**.

정적 페이지가 아닌, 한 인간의 투명한 데이터 연결체.
대문은 org-agenda 문. 그 안에 존재의 데이터와 에이전트들이 시간 축에서 살아있다.

## 무엇을 보여주는가

| 데이터 | 규모 | 형식 |
|--------|------|------|
| Denote 노트 | 3,000+ | .org |
| 수면/심박/시간추적 | 4,214+ | SQLite |
| 서지 데이터 | 7,000+ | .bib |
| 일일 저널 | 696일+ | .org |
| Git 커밋 | 14,000+ | git |
| 디지털 가든 | 1,400+ | .md |

## 기술 스택

```
[프론트]                      [백엔드]                    [데이터]
WebTUI CSS                    Clojure 서버                Emacs daemon
+ SF 터미널 미학              (Babashka 또는 Ring)        emacsclient
+ ASCII box org-agenda        /api/agenda                 ~/org/
+ SSE 실시간 갱신             /api/events (SSE)           lifetract.db
                              /api/trigger                .bib
```

## 디자인 — SF 터미널 미학

**레트로가 아니다.** TRON, Blade Runner의 미래형 터미널.

- 색상: 발광 네온 그린 `#50fa7b`, 시안 `#8be9fd`, 깊은 다크 `#0a0a1a`
- 폰트: Geist Pixel (제목) + Geist Mono + Pretendard (본문)
- 레이아웃: `ch`/`lh` 캐릭터 기반 그리드

## 개발

```bash
nix develop          # 개발 환경 진입
clj -M:dev           # nREPL 시작
bb dev               # Babashka 개발 서버 (예정)
```

## 이름의 유래

**geworfen** [ɡəˈvɔʁfn̩] = 게보르펜 = "던져진"

하이데거의 Geworfenheit(던져짐/피투성)에서.
org-agenda에 찍히는 스탬프 하나하나가 세계 안에 던져진 사실성(Faktizität).
가공 없이 던져져 있는 날것의 데이터. 그것이 이 프로젝트다.

## 링크

- 📚 [디지털 가든](https://notes.junghanacs.com)
- 🐙 [GitHub](https://github.com/junghanacs)
- 🧵 [Threads](https://www.threads.net/@junghanacs)

## 라이선스

MIT
