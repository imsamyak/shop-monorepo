// INGEST: prepare the knowledge base. Run it with `npm run ingest`.
//
// Steps:
//   1. Read every .md file in the knowledge/ folder (shop policies, FAQ...)
//   2. Cut each file into small chunks (a few paragraphs each)
//   3. Turn each chunk into a vector (embedder.js)
//   4. Save everything to data/index.json
//
// Run it again whenever you edit the knowledge files.

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { embed } from "./embedder.js";
import { INDEX_FILE } from "./vectorStore.js";

const KNOWLEDGE_DIR = new URL("../../knowledge/", import.meta.url);
const MAX_CHUNK_CHARS = 500;

// Split text on blank lines (paragraphs), then glue paragraphs together
// until a chunk reaches about MAX_CHUNK_CHARS characters.
function splitIntoChunks(text) {
  const paragraphs = text.split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean);
  const chunks = [];
  let current = "";
  for (const p of paragraphs) {
    if (current && current.length + p.length > MAX_CHUNK_CHARS) {
      chunks.push(current);
      current = "";
    }
    current += (current ? "\n\n" : "") + p;
  }
  if (current) chunks.push(current);
  return chunks;
}

const files = fs.readdirSync(KNOWLEDGE_DIR).filter((f) => f.endsWith(".md"));
const index = [];

for (const file of files) {
  const text = fs.readFileSync(new URL(file, KNOWLEDGE_DIR), "utf8");
  for (const chunk of splitIntoChunks(text)) {
    index.push({ source: file, text: chunk, vector: await embed(chunk) });
  }
  console.log(`Indexed ${file}`);
}

fs.mkdirSync(path.dirname(fileURLToPath(INDEX_FILE)), { recursive: true });
fs.writeFileSync(INDEX_FILE, JSON.stringify(index));
console.log(`Done: ${index.length} chunks saved to data/index.json`);
