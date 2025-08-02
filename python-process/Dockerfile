# ... (начало файла без изменений: FROM, RUN apt-get, WORKDIR) ...
FROM python:3.11-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app

# --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
# Сначала копируем модель, чтобы она кешировалась как отдельный слой
COPY ./models/whisper-large-v3 /app/models/whisper-large-v3

# Теперь копируем зависимости и код
COPY requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY . .

# Команда для запуска (с флагом -u для логов)
CMD ["python", "-u", "process_consumer.py"]
