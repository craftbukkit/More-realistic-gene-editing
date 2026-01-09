{ pkgs, ... }: {
  channel = "unstable";
  packages = [
    pkgs.jdk21
    pkgs.gradle
    pkgs.nodejs_20
    pkgs.go
    pkgs.livekit-server
  ];
  env = {
    LIVEKIT_API_KEY = "devkey";
    LIVEKIT_API_SECRET = "secret";
    JAVA_HOME = "${pkgs.jdk21}";
  };
  idx = {
    extensions = [
      "redhat.java"
      "vscodevim.vim"
      "golang.go"
      "dbaeumer.vscode-eslint"
    ];
    workspace = {
      onStart = {
        gen-sources = "cd GenomeWorkbench && export JAVA_HOME=${pkgs.jdk21} && gradle genSources";
        auth = "cd apps/auth && go run .";
        kms = "cd apps/kms && go run .";
        web = "cd apps/web && npm install && npm run dev";
        livekit = "livekit-server --dev";
      };
    };
    previews = {
      enable = true;
      previews = {
        web = {
          command = ["npm" "run" "dev" "--" "--port" "$PORT"];
          manager = "web";
          cwd = "apps/web";
        };
      };
    };
  };
}
