from flask import Flask, request, jsonify
import torch
import clip
from PIL import Image
import faiss
import os
import pickle

app = Flask(__name__)

# ---------------------------
# CONFIG
# ---------------------------
INDEX_FILE = "faiss.index"
PATHS_FILE = "image_paths.pkl"
DIM = 512

# ---------------------------
# LOAD MODEL
# ---------------------------
model, preprocess = clip.load("ViT-B/32")

# ---------------------------
# LOAD OR CREATE FAISS
# ---------------------------
if os.path.exists(INDEX_FILE) and os.path.exists(PATHS_FILE):

    print("Loading FAISS index from disk...")

    index = faiss.read_index(INDEX_FILE)

    with open(PATHS_FILE, "rb") as f:
        image_paths = pickle.load(f)

    print("Loaded vectors:", index.ntotal)

else:

    print("Creating new FAISS index...")

    # cosine similarity
    index = faiss.IndexFlatIP(DIM)

    image_paths = []

# ---------------------------
# IMAGE EMBEDDING
# ---------------------------
def get_image_embedding(path):

    image = Image.open(path).convert("RGB")

    image = preprocess(image).unsqueeze(0)

    with torch.no_grad():
        emb = model.encode_image(image)

    # normalize embedding
    emb = emb / emb.norm(dim=-1, keepdim=True)

    return emb.cpu().numpy().astype("float32")

# ---------------------------
# TEXT EMBEDDING
# ---------------------------
def get_text_embedding(text):

    tokens = clip.tokenize([text])

    with torch.no_grad():
        emb = model.encode_text(tokens)

    # normalize embedding
    emb = emb / emb.norm(dim=-1, keepdim=True)

    return emb.cpu().numpy().astype("float32")

# ---------------------------
# SAVE FAISS
# ---------------------------
def save_index():

    faiss.write_index(index, INDEX_FILE)

    with open(PATHS_FILE, "wb") as f:
        pickle.dump(image_paths, f)

# ---------------------------
# STORE IMAGE
# ---------------------------
@app.route("/store", methods=["POST"])
def store():

    data = request.json

    if not data or "image_path" not in data:
        return jsonify({"error": "Invalid request"}), 400

    path = data["image_path"]

    try:

        emb = get_image_embedding(path)

        index.add(emb)

        image_paths.append(path)

        save_index()

        print("Stored:", path)
        print("Total vectors:", index.ntotal)

        return jsonify({
            "status": "stored",
            "total": index.ntotal
        })

    except Exception as e:

        print("Store error:", e)

        return jsonify({
            "error": str(e)
        }), 500

# ---------------------------
# SEARCH
# ---------------------------
@app.route("/search", methods=["POST"])
def search():

    if len(image_paths) == 0:
        return jsonify({"results": []})

    data = request.json

    query = data.get("query", "")

    try:

        qvec = get_text_embedding(query)

        k = min(5, len(image_paths))

        D, I = index.search(qvec, k)

        results = []

        # cosine similarity threshold
        THRESHOLD = 0.24

        for idx, score in zip(I[0], D[0]):

            print("Index:", idx, "Score:", score)

            if idx >= 0 and idx < len(image_paths):

                # higher score = better match
                if score >= THRESHOLD:

                    results.append(image_paths[idx])

        # remove duplicates
        results = list(dict.fromkeys(results))

        return jsonify({
            "results": results
        })

    except Exception as e:

        print("Search error:", e)

        return jsonify({
            "results": []
        })

# ---------------------------
# RUN SERVER
# ---------------------------
if __name__ == "__main__":

    app.run(port=5000)