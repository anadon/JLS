{
  # Nix flake so NixOS (and any Nix user) can build and run JLS from
  # source — the deb/rpm/AppImage release assets do not fit NixOS:
  #
  #   nix run github:anadon/JLS -- circuit.jls     # run straight from GitHub
  #   nix profile install github:anadon/JLS        # install the `jls` command
  #   nix develop                                  # JDK 25 + Maven dev shell
  #
  # The build is `mvn package` against the pinned nixpkgs JDK (Java 25,
  # the pom's maven.compiler.release); the test suite is CI's job
  # (`mvn verify` there), not the sandbox's — see mvnParameters below.
  description = "JLS — an educational digital logic circuit editor and simulator";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";

  outputs = { self, nixpkgs }:
    let
      # a Swing GUI application: only systems someone has actually run it
      # on are listed; add more once tested
      systems = [ "x86_64-linux" "aarch64-linux" ];
      eachSystem = f:
        nixpkgs.lib.genAttrs systems
          (system: f system nixpkgs.legacyPackages.${system});
    in
    {
      packages = eachSystem (system: pkgs:
        let
          jdk = pkgs.jdk25;

          jls = pkgs.maven.buildMavenPackage {
            pname = "jls";
            # keep in sync with pom.xml; the base triple is enough — a
            # -SNAPSHOT suffix between releases changes nothing here
            version = "5.0.4";
            src = self;

            # fingerprint of the Maven dependency closure. Refresh after
            # any pom dependency/plugin change: set to nixpkgs.lib.fakeHash,
            # run `nix build`, copy the hash from the "got:" line.
            mvnHash = "sha256-38VZjujqJNg7GEgMxWU+PD9FXp+hfladPrycFLMixHc=";

            # Maven must run on the pom's language baseline (Java 25, and
            # the enforcer rejects anything older)
            mvnJdk = pkgs.jdk25_headless;

            # tests run in CI under `mvn verify`; the nix sandbox has no
            # fonts, which the geometry/golden tests are sensitive to
            # (same reasoning as the Windows installer leg, issue #111)
            doCheck = false;

            nativeBuildInputs = [ pkgs.makeWrapper pkgs.copyDesktopItems ];

            desktopItems = [
              (pkgs.makeDesktopItem {
                name = "jls";
                exec = "jls %f";
                icon = "jls";
                desktopName = "JLS";
                comment = "Educational digital logic circuit editor and simulator";
                categories = [ "Education" "Electronics" ];
                mimeTypes = [ "application/x-jls-circuit" ];
              })
            ];

            installPhase = ''
              runHook preInstall
              install -Dm644 target/jls-*.jar $out/share/jls/jls.jar
              makeWrapper ${jdk}/bin/java $out/bin/jls \
                --add-flags "-jar $out/share/jls/jls.jar"
              install -Dm644 resources/packaging/jls.png \
                $out/share/icons/hicolor/256x256/apps/jls.png
              runHook postInstall
            '';

            meta = {
              description = "Educational digital logic circuit editor and simulator";
              homepage = "https://github.com/anadon/JLS";
              license = nixpkgs.lib.licenses.gpl3Only;
              mainProgram = "jls";
            };
          };
        in
        {
          inherit jls;
          default = jls;
        });

      apps = nixpkgs.lib.genAttrs systems (system: rec {
        jls = {
          type = "app";
          program = "${self.packages.${system}.jls}/bin/jls";
        };
        default = jls;
      });

      devShells = eachSystem (system: pkgs: {
        default = pkgs.mkShell {
          # Maven wrapped over the full (non-headless) JDK 25, so mvn,
          # jdeps, jlink and jpackage all agree on the toolchain
          packages = [
            (pkgs.maven.override { jdk_headless = pkgs.jdk25; })
            pkgs.jdk25
          ];
        };
      });
    };
}
