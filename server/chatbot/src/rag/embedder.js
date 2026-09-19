// EMBEDDINGS: turn a piece of text into a list of numbers (a "vector").
//
// Texts with similar MEANING get vectors that point in similar directions.
// "How long does delivery take?" and "shipping time" end up close together,
// even though they share no words. That is what makes RAG search smart.
//
// We use a small free model that runs on your own machine (no API key needed).
// The first run downloads it once (~25 MB), then it is cached.

import { pipeline } from "@huggingface/transformers";

let extractor = null; // loaded once, then reused

export async function embed(text) {
  if (!extractor) {
    extractor = await pipeline("feature-extraction", "Xenova/all-MiniLM-L6-v2");
  }
  // pooling: "mean" -> one vector for the whole text
  // normalize: true -> every vector has length 1, which makes comparing them easy
  const output = await extractor(text, { pooling: "mean", normalize: true });
  return Array.from(output.data); // 384 numbers
}
