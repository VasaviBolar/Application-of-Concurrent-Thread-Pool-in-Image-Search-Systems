from flask import Flask, request, jsonify
import torch
import clip
from PIL import Image
import faiss
import numpy as np

app = Flask(__name__)

model, preprocess = clip.load("ViT-B/32")

index = faiss.IndexFlatL2(512)
image_paths = []

def get_image_embedding(path):
    image = preprocess(Image.open(path)).unsqueeze(0)
    with torch.no_grad():
        emb = model.encode_image(image)
    return emb.numpy().astype("float32")

def get_text_embedding(text):
    tokens = clip.tokenize([text])
    with torch.no_grad():
        emb = model.encode_text(tokens)
    return emb.numpy().astype("float32")

@app.route("/store", methods=["POST"])
def store():
    path = request.json["image_path"]

    emb = get_image_embedding(path)
    index.add(emb)
    image_paths.append(path)

    return jsonify({"status": "stored"})

@app.route("/search", methods=["POST"])
def search():
    query = request.json["query"]

    qvec = get_text_embedding(query)

    D, I = index.search(qvec, 3)

    results = [image_paths[i] for i in I[0]]

    return jsonify({"results": results})

app.run(port=5000)