{
  description = "geworfen — existence data WebTUI viewer";

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
            echo "geworfen — thrown into the world"
            echo "================================"
            echo "Clojure: $(clojure --version 2>&1)"
            echo "Java:    $(java -version 2>&1 | head -1)"
            echo "bb:      $(bb --version 2>&1)"
            echo ""
            echo "  bb dev             # dev server"
            echo "  bb repl            # nREPL (CIDER)"
            echo "  clj -M:run         # production server"
            echo ""
          '';
        };
      }
    );
}
