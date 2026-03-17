{
  description = "geworfen — existence data WebTUI viewer (GraalVM native)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        graalvm = pkgs.graalvmPackages.graalvm-ce;

        fhsEnv = pkgs.buildFHSEnv {
          name = "geworfen-build";
          targetPkgs = pkgs: with pkgs; [
            clojure
            graalvm
            zlib
            glibc
            glibc.static
          ];
          runScript = pkgs.writeShellScript "geworfen-build-init" ''
            export JAVA_HOME=${graalvm}
            export GRAALVM_HOME=${graalvm}
            exec bash "$@"
          '';
        };
      in
      {
        packages.fhs = fhsEnv;

        devShells = {
          # Default: GraalVM (native-image build)
          default = pkgs.mkShell {
            name = "geworfen";
            buildInputs = with pkgs; [ clojure graalvm babashka ];
            JAVA_HOME = graalvm;
            GRAALVM_HOME = graalvm;
            shellHook = ''
              echo "geworfen — thrown into the world"
              echo "================================"
              echo "GraalVM: $(native-image --version 2>/dev/null | head -1)"
              echo ""
              echo "  clj -M:run              # JVM server"
              echo "  clj -T:build uber       # uberjar"
              echo "  ./run.sh build          # native binary"
              echo "  ./run.sh serve          # run native binary"
              echo ""
            '';
          };

          # JVM only (lightweight dev)
          jvm = pkgs.mkShell {
            name = "geworfen-jvm";
            buildInputs = with pkgs; [ clojure jdk17_headless babashka ];
            shellHook = ''
              echo "geworfen dev (JVM only)"
            '';
          };
        };
      });
}
