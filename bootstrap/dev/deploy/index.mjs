// Deploys the development shop to one server over ssh. Run from the repository root: pnpm deploy:dev <command>

import * as commands from './commands.mjs';

const help = `pnpm deploy:dev <command>

  setup           once per server: grow the disk, add swap, install Docker
  up              copy the code and .env, build every image, start the stack, seed it the first time
  status          containers, memory and disk on the server
  logs [service]  follow the logs
  url             print the address of the shop
  ingest          seed the sellers and products again
  down [--volumes] stop everything (--volumes also deletes the databases)
  shell           open a terminal on the server

The server and key are in bootstrap/dev/deploy.config.json.`;

const [name, ...args] = process.argv.slice(2);
if (!name || !(name in commands)) {
  console.log(help);
  process.exit(name ? 1 : 0);
}

commands[name](...args).catch((error) => {
  console.error(`\n${name} failed: ${error.message}`);
  process.exit(1);
});
