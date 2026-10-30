import { spawn } from 'node:child_process';

// ssh and scp come with Windows 10+, macOS and Linux, so the tool has no dependencies.
const options = (config) => [
  '-i', config.keyPath,
  '-o', 'StrictHostKeyChecking=accept-new',
  '-o', 'ServerAliveInterval=30',
  '-o', 'ServerAliveCountMax=40',
];
const target = (config) => `${config.user}@${config.host}`;

function exec(command, args, stdio = 'inherit') {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio });
    child.on('error', reject);
    child.on('close', (code) => (code === 0 ? resolve() : reject(new Error(`${command} exited with ${code}`))));
  });
}

/** Runs a bash script on the server. It travels base64-encoded in the command line, so there is no quoting to get wrong. */
export function run(config, script) {
  const body = Buffer.from(`set -euo pipefail\n${script}\n`).toString('base64');
  return exec('ssh', [...options(config), target(config), `echo ${body} | base64 -d > /tmp/deploy.sh && bash /tmp/deploy.sh`]);
}

/** Opens a terminal on the server (for logs -f or a shell). */
export function terminal(config, command = '') {
  return exec('ssh', [...options(config), '-t', target(config), command]);
}

export function upload(config, local, remote) {
  return exec('scp', [...options(config), local, `${target(config)}:${remote}`]);
}

/** Copies a folder to the server as one compressed stream (tar over ssh), leaving out what the server builds itself. */
export function syncFolder(config, localDir, remoteDir, excludes) {
  const tar = spawn('tar', ['-czf', '-', ...excludes.map((pattern) => `--exclude=${pattern}`), '-C', localDir, '.'], {
    stdio: ['ignore', 'pipe', 'inherit'],
  });
  const done = (child, name) =>
    new Promise((resolve, reject) => {
      child.on('error', reject);
      child.on('exit', (code) => (code === 0 ? resolve() : reject(new Error(`${name} exited with ${code}`))));
    });
  const ssh = spawn('ssh', [...options(config), target(config), `mkdir -p ${remoteDir} && tar -xzf - -C ${remoteDir}`], {
    stdio: ['pipe', 'inherit', 'inherit'],
  });
  tar.stdout.pipe(ssh.stdin);
  return Promise.all([done(tar, 'tar'), done(ssh, 'ssh')]);
}
