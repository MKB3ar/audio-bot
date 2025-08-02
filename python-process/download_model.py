from huggingface_hub import snapshot_download

model_id = "openai/whisper-large-v3"
local_dir = "./models/whisper-large-v3"

print(f"Скачивание модели {model_id} в папку {local_dir}...")
snapshot_download(
    repo_id=model_id,
    local_dir=local_dir,
    local_dir_use_symlinks=False, # Важно для Docker
    resume_download=True
)
print("Модель успешно скачана!")
