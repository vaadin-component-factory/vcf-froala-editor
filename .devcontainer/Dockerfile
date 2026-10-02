# syntax=docker/dockerfile:1
# Node 24+ required by Vaadin 25.2 frontend tooling. Pinned to the bookworm
# (Debian 12) variant because the Playwright system libraries installed below
# use bookworm package names (e.g. libasound2, renamed on trixie).
FROM node:24-bookworm

ARG TZ
ENV TZ="$TZ"
ARG CLAUDE_CODE_VERSION=latest

# Install basic development tools
RUN apt-get update && apt-get install -y --no-install-recommends \
  less \
  git \
  procps \
  sudo \
  fzf \
  zsh \
  man-db \
  unzip \
  gnupg2 \
  iproute2 \
  dnsutils \
  jq \
  nano \
  vim \
  wget \
  apt-transport-https \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install OpenJDK 25 (architecture-aware)
RUN ARCH=$(dpkg --print-architecture) && \
  if [ "$ARCH" = "amd64" ]; then JDK_ARCH="x64"; else JDK_ARCH="aarch64"; fi && \
  wget -q "https://download.java.net/java/GA/jdk25.0.2/b1e0dfa218384cb9959bdcb897162d4e/10/GPL/openjdk-25.0.2_linux-${JDK_ARCH}_bin.tar.gz" -O /tmp/openjdk25.tar.gz && \
  mkdir -p /usr/lib/jvm && \
  tar -xzf /tmp/openjdk25.tar.gz -C /usr/lib/jvm && \
  mv /usr/lib/jvm/jdk-25.0.2 /usr/lib/jvm/openjdk-25 && \
  rm /tmp/openjdk25.tar.gz

ENV JAVA_HOME=/usr/lib/jvm/openjdk-25
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install Maven
ARG MAVEN_VERSION=3.9.9
RUN wget -qO- https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xzf - -C /opt && \
  ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven

ENV MAVEN_HOME=/opt/maven
ENV PATH="$MAVEN_HOME/bin:$PATH"

# Install Playwright system dependencies (Chromium) - for Debian 12 bookworm
RUN apt-get update && apt-get install -y --no-install-recommends \
  libnss3 \
  libnspr4 \
  libatk1.0-0 \
  libatk-bridge2.0-0 \
  libcups2 \
  libdrm2 \
  libdbus-1-3 \
  libxkbcommon0 \
  libatspi2.0-0 \
  libxcomposite1 \
  libxdamage1 \
  libxfixes3 \
  libxrandr2 \
  libgbm1 \
  libasound2 \
  libpango-1.0-0 \
  libcairo2 \
  libx11-6 \
  libx11-xcb1 \
  libxcb1 \
  libxext6 \
  libxshmfence1 \
  libglib2.0-0 \
  libexpat1 \
  libfontconfig1 \
  libudev1 \
  fonts-liberation \
  fonts-noto-color-emoji \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Google Chrome, for the Playwright MCP. It starts the `chrome` channel and looks for a real
# Chrome at /opt/google/chrome/chrome; Playwright's own Chromium under ~/.cache/ms-playwright —
# what the Java e2e tests drive — does not satisfy it. Unpinned on purpose: Chrome is a rolling
# stable channel with no archive of old .debs, and this is what `playwright install chrome` does.
# ⚠️ Google ships no linux-arm64 build, so on that architecture the MCP has no browser and the
# Java tests are the only way into one.
RUN ARCH=$(dpkg --print-architecture); \
  if [ "$ARCH" = "amd64" ]; then \
    wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
      -O /tmp/chrome.deb && \
    apt-get update && \
    apt-get install -y --no-install-recommends /tmp/chrome.deb && \
    rm /tmp/chrome.deb && \
    apt-get clean && rm -rf /var/lib/apt/lists/*; \
  else \
    echo "No Google Chrome build for linux-$ARCH; the Playwright MCP will not start here."; \
  fi

# Ensure default node user has access to /usr/local/share
RUN mkdir -p /usr/local/share/npm-global && \
  chown -R node:node /usr/local/share

ARG USERNAME=node

# Persist bash history.
RUN SNIPPET="export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
  && mkdir /commandhistory \
  && touch /commandhistory/.bash_history \
  && chown -R $USERNAME /commandhistory

# Set `DEVCONTAINER` environment variable to help with orientation
ENV DEVCONTAINER=true

# The workspace is bind-mounted at the host's own path (devcontainer.json), so the image
# creates no directory for it; only the config directory is the image's to make.
RUN mkdir -p /home/node/.claude && \
  chown -R node:node /home/node/.claude

WORKDIR /home/node

# Install Git Delta (Dynamic Architecture)
ARG GIT_DELTA_VERSION=0.18.2
RUN ARCH=$(dpkg --print-architecture) && \
  wget "https://github.com/dandavison/delta/releases/download/${GIT_DELTA_VERSION}/git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  sudo dpkg -i "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb" && \
  rm "git-delta_${GIT_DELTA_VERSION}_${ARCH}.deb"

# Install GitHub CLI (Dynamic Architecture). Not from apt: Debian bookworm ships 2.23.0 (Feb 2023),
# which checks the X-OAuth-Scopes header that fine-grained tokens never send, so `gh issue create`
# and `gh issue edit` reject a valid token.
ARG GH_VERSION=2.101.0
RUN ARCH=$(dpkg --print-architecture) && \
  wget -q "https://github.com/cli/cli/releases/download/v${GH_VERSION}/gh_${GH_VERSION}_linux_${ARCH}.deb" -O gh.deb && \
  sudo dpkg -i gh.deb && \
  rm gh.deb

# Install GitLab CLI (Dynamic Architecture)
ARG GLAB_VERSION=1.117.0
RUN ARCH=$(dpkg --print-architecture) && \
  wget -q "https://gitlab.com/api/v4/projects/gitlab-org%2Fcli/packages/generic/glab/${GLAB_VERSION}/glab_${GLAB_VERSION}_linux_${ARCH}.deb" -O glab.deb && \
  sudo dpkg -i glab.deb && \
  rm glab.deb

# Grant sudo access to node user
RUN echo "node ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers.d/node && chmod 0440 /etc/sudoers.d/node

# Set up non-root user
USER node

# Install global packages
ENV NPM_CONFIG_PREFIX=/usr/local/share/npm-global
ENV PATH=$PATH:/usr/local/share/npm-global/bin

# Set the default shell to zsh rather than sh
ENV SHELL=/bin/zsh

# Set the default editor and visual
ENV EDITOR=nano
ENV VISUAL=nano

# Default powerline10k theme
ARG ZSH_IN_DOCKER_VERSION=1.2.0
RUN sh -c "$(wget -O- https://github.com/deluan/zsh-in-docker/releases/download/v${ZSH_IN_DOCKER_VERSION}/zsh-in-docker.sh)" -- \
  -p git \
  -p fzf \
  -a "source /usr/share/doc/fzf/examples/key-bindings.zsh" \
  -a "source /usr/share/doc/fzf/examples/completion.zsh" \
  -a "export PROMPT_COMMAND='history -a' && export HISTFILE=/commandhistory/.bash_history" \
  -a "unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY no_proxy NO_PROXY" \
  -x

# Install Claude Code
RUN npm install -g @anthropic-ai/claude-code@${CLAUDE_CODE_VERSION}

# ensure latest version
RUN claude update

# Install official plugins (marketplace must be registered explicitly during build)
RUN claude plugin marketplace add anthropics/claude-plugins-official --scope user && \
    claude plugin install commit-commands@claude-plugins-official --scope user && \
    claude plugin install security-guidance@claude-plugins-official --scope user && \
    claude plugin install jdtls-lsp@claude-plugins-official --scope user && \
    claude plugin install typescript-lsp@claude-plugins-official --scope user && \
    claude plugin install playwright@claude-plugins-official --scope user && \
    claude plugin install mattpocock-skills@claude-plugins-official --scope user

# ponytail lives in its own third-party marketplace (github.com/DietrichGebert/ponytail),
# so register that marketplace before installing the plugin from it.
RUN claude plugin marketplace add DietrichGebert/ponytail --scope user && \
    claude plugin install ponytail@ponytail --scope user

# Verify every expected plugin is actually installed. `claude plugin install`
# can return success even when a network-sourced plugin fails to materialize,
# so fail the build loudly here instead of shipping an image with missing plugins.
RUN set -e; \
    expected="commit-commands@claude-plugins-official \
              security-guidance@claude-plugins-official \
              jdtls-lsp@claude-plugins-official \
              typescript-lsp@claude-plugins-official \
              playwright@claude-plugins-official \
              mattpocock-skills@claude-plugins-official \
              ponytail@ponytail"; \
    installed="$(claude plugin list)"; \
    missing=""; \
    for p in $expected; do \
        if ! echo "$installed" | grep -qF "$p"; then \
            missing="$missing $p"; \
        fi; \
    done; \
    if [ -n "$missing" ]; then \
        echo "ERROR: the following plugins failed to install:$missing"; \
        echo "--- claude plugin list output ---"; \
        echo "$installed"; \
        exit 1; \
    fi; \
    echo "All expected plugins installed successfully."