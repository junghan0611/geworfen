{
  description = "being-viewer — Clerk 기반 존재 데이터 뷰어";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.clojure
            pkgs.jdk17
            pkgs.babashka
          ];

          shellHook = ''
            echo "🏠 being-viewer 개발 환경"
            echo "========================"
            echo "Clojure: $(clojure --version 2>&1)"
            echo "Java:    $(java -version 2>&1 | head -1)"
            echo ""
            echo "  clj -M:dev            # nREPL 시작"
            echo "  clojure -M -e \"(require '[nextjournal.clerk :as clerk]) (clerk/serve! {:browse? true :port 7777 :watch-paths [\\\"notebooks\\\" \\\"src\\\"]})\""
            echo ""
          '';
        };
      }
    );
}
