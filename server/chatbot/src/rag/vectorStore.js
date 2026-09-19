// VECTOR STORE: keeps our text chunks + their vectors, and finds the closest ones.
//
// Real projects use a vector database (pgvector, Pinecone, Chroma...).
// To keep things simple we store everything in one JSON file and search in memory.
// For a few hundred chunks this is instant.

import fs from "node:fs";
import { embed } from "./embedder.js";

export const INDEX_FILE = new URL("../../data/index.json", import.meta.url);

let chunks = []; // [{ source, text, vector }]

export function loadIndex() {
  if (!fs.existsSync(INDEX_FILE)) {
    console.warn("No RAG index found. Run `pnpm ingest` first.");
    return;
  }
  chunks = JSON.parse(fs.readFileSync(INDEX_FILE, "utf8"));
  console.log(`RAG index loaded: ${chunks.length} chunks`);
}

// How similar are two vectors? Because they are normalized, the dot product
// is the "cosine similarity": 1 = same meaning, 0 = unrelated.
function similarity(a, b) {
  let sum = 0;
  for (let i = 0; i < a.length; i++) sum += a[i] * b[i];
  return sum;
}

// The "R" in RAG: Retrieve the top K chunks most related to the question.
export async function search(question, topK = 3) {
  const questionVector = await embed(question);
  return chunks
    .map((chunk) => ({ source: chunk.source, text: chunk.text, score: similarity(questionVector, chunk.vector) }))
    .sort((a, b) => b.score - a.score)
    .slice(0, topK);
}
