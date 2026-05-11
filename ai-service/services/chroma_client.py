import chromadb
from chromadb.utils import embedding_functions
import uuid

class ChromaClient:

    def __init__(self):
        self.client = chromadb.Client()

        # Use chromadb's built-in ONNX-based embedding function (all-MiniLM-L6-v2)
        # This is functionally equivalent to SentenceTransformer but runs via ONNX Runtime
        # (CPU-only, ~50MB) instead of PyTorch + CUDA (~700MB+)
        self.embed_fn = embedding_functions.DefaultEmbeddingFunction()

        self.collection = self.client.get_or_create_collection(
            name="audit_docs",
            embedding_function=self.embed_fn
        )

    def add_documents(self, docs):
        chunked_docs = []
        chunk_size = 500
        overlap = 50
        
        for doc in docs:
            start = 0
            doc_len = len(doc)
            
            if doc_len == 0:
                continue
                
            while start < doc_len:
                end = min(start + chunk_size, doc_len)
                chunked_docs.append(doc[start:end])
                
                if end == doc_len:
                    break
                    
                start += (chunk_size - overlap)

        if not chunked_docs:
            return

        # Embeddings are now handled automatically by the collection's embedding_function
        ids = [str(uuid.uuid4()) for _ in range(len(chunked_docs))]

        self.collection.add(
            documents=chunked_docs,
            ids=ids
        )

    def query(self, text, n_results=3):
        # Embedding handled automatically by the collection
        results = self.collection.query(
            query_texts=[text],
            n_results=n_results
        )

        return results["documents"]