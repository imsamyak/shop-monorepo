import { envFile, loadConfig, repoRoot } from './lib/config.mjs';
import { run, syncFolder, terminal, upload } from './lib/ssh.mjs';

const compose = 'docker compose -f bootstrap/dev/docker-compose.yml --env-file bootstrap/dev/.env';
const ingestedMarker = '$HOME/.shop-ingested';

// what the server builds or installs itself, or must never receive (the secrets are uploaded separately)
const notUploaded = ['node_modules', '.git', '.next', 'target', 'dist', '.idea', '.claude', '.env'];

/** The environment of every compose command: the tunnel switch decides the profile and whether the cookie is Secure. */
const composeEnv = (config) =>
  `export COMPOSE_PROFILES=${config.tunnel ? 'tunnel' : ''} COOKIE_SECURE=${config.tunnel ? 'true' : 'false'}`;

const inRepo = (config, script) => `cd "$HOME/${config.dir}"\n${composeEnv(config)}\n${script}`;

/** Once per server: grow the disk, add swap, install Docker and its compose and buildx plugins. Safe to run again. */
export async function setup() {
  const config = loadConfig();
  console.log(`Preparing ${config.user}@${config.host}`);
  await run(config, `
echo "== disk: grow the root partition and filesystem to the size of the volume"
SOURCE=$(findmnt -n -o SOURCE /)
DISK=/dev/$(lsblk -no PKNAME "$SOURCE")
PART=$(echo "$SOURCE" | grep -oE '[0-9]+$')
sudo growpart "$DISK" "$PART" || true
sudo xfs_growfs / > /dev/null
df -h /

echo "== swap: 4 GB, so the Java builds and the services fit in the machine's memory"
if [ -z "$(swapon --show=NAME --noheadings)" ]; then
  sudo fallocate -l 4G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile > /dev/null
  sudo swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab > /dev/null
fi
free -m | sed -n 1,3p

echo "== docker"
sudo dnf install -y docker > /dev/null
sudo mkdir -p /etc/docker
# container logs are capped, so they cannot fill the disk
echo '{"log-driver":"json-file","log-opts":{"max-size":"10m","max-file":"3"}}' | sudo tee /etc/docker/daemon.json > /dev/null
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

PLUGINS=/usr/local/lib/docker/cli-plugins
sudo mkdir -p $PLUGINS
ARCH=$(uname -m)
if [ ! -x $PLUGINS/docker-compose ]; then
  sudo curl -fsSL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$ARCH" -o $PLUGINS/docker-compose
  sudo chmod +x $PLUGINS/docker-compose
fi
if [ ! -x $PLUGINS/docker-buildx ]; then
  curl -fsSL https://api.github.com/repos/docker/buildx/releases/latest -o /tmp/buildx-release.json
  TAG=$(grep -m1 '"tag_name"' /tmp/buildx-release.json | cut -d'"' -f4)
  case "$ARCH" in aarch64) BUILDX_ARCH=arm64 ;; *) BUILDX_ARCH=amd64 ;; esac
  sudo curl -fsSL "https://github.com/docker/buildx/releases/download/$TAG/buildx-$TAG.linux-$BUILDX_ARCH" -o $PLUGINS/docker-buildx
  sudo chmod +x $PLUGINS/docker-buildx
fi
sudo docker --version
sudo docker compose version
sudo docker buildx version
echo "== ready"
`);
}

/** Copies the code and the .env, builds every image (one at a time), starts the stack, and seeds it the first time. */
export async function up() {
  const config = loadConfig();
  console.log('Copying the code');
  await syncFolder(config, repoRoot, config.dir, notUploaded);
  console.log(`Uploading ${envFile()} (not printed)`);
  await upload(config, envFile(), `${config.dir}/bootstrap/dev/.env`);

  await run(config, inRepo(config, `
chmod 600 bootstrap/dev/.env
# one build at a time: seven Maven builds together would not fit in the memory
for service in $(${compose} config --services); do
  echo "== building $service"
  ${compose} build "$service"
done
echo "== starting"
${compose} up -d --wait --wait-timeout 900
if [ ! -f ${ingestedMarker} ]; then
  echo "== seeding the sellers and products"
  ${compose} run --rm -T ingest < /dev/null
  touch ${ingestedMarker}
fi
${compose} ps
echo
echo "Open ${config.tunnel ? 'the address from: pnpm deploy:dev url' : `http://${config.host}`}"
`));
}

export async function status() {
  const config = loadConfig();
  await run(config, inRepo(config, `${compose} ps -a\nfree -m | sed -n 1,3p\ndf -h / | tail -1`));
}

export async function logs(service = '') {
  const config = loadConfig();
  await terminal(config, `cd "$HOME/${config.dir}" && ${composeEnv(config)} && ${compose} logs -f --tail=100 ${service}`);
}

export async function url() {
  const config = loadConfig();
  if (!config.tunnel) return console.log(`http://${config.host}`);
  await run(config, inRepo(config, `${compose} logs cloudflared | grep -o 'https://[a-z0-9-]*\\.trycloudflare\\.com' | tail -1`));
}

/** Seeds again; the sellers must not exist yet (see scripts/README.md for the reset). */
export async function ingest() {
  const config = loadConfig();
  await run(config, inRepo(config, `${compose} run --rm -T ingest < /dev/null\ntouch ${ingestedMarker}`));
}

/** Stops everything. The data is kept; --volumes deletes the databases too (the next `up` seeds again). */
export async function down(flag = '') {
  const config = loadConfig();
  const volumes = flag === '--volumes';
  await run(config, inRepo(config, `${compose} down ${volumes ? '--volumes' : ''}${volumes ? `\nrm -f ${ingestedMarker}` : ''}`));
}

export async function shell() {
  await terminal(loadConfig());
}
