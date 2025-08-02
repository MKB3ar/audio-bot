# process_consumer.py

import json
import os
import sys

from kafka import KafkaConsumer, KafkaProducer
from transformers import pipeline
import torch

# --- КОНФИГУРАЦИЯ ---
PROCESS_TOPIC_NAME = 'process'
DONE_TOPIC_NAME = 'done'
KAFKA_BOOTSTRAP_SERVERS = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'kafka:9092')
UPLOADS_BASE_DIR = os.getenv('APP_BASE_DIR', '/data/uploads')
#KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
#UPLOADS_BASE_DIR = '../data/uploads'


# --- Инициализация Kafka Consumer и Producer ---
consumer = KafkaConsumer(
    PROCESS_TOPIC_NAME,
    bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
    auto_offset_reset='earliest',
    enable_auto_commit=True,
    group_id='text-processor-group',
    value_deserializer=lambda m: json.loads(m.decode('utf-8'))
)

producer = KafkaProducer(
    bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

# --- Инициализация модели Whisper (один раз при старте) ---
device = "cuda" if torch.cuda.is_available() else "cpu"
torch_dtype = torch.float16 if torch.cuda.is_available() else torch.float32
model_id = "/app/models/whisper-large-v3"

print(f"[*] Загрузка локальной модели из '{model_id}' на устройство '{device}'...")
try:
    pipe = pipeline(
        "automatic-speech-recognition",
        model=model_id, # <-- Передаем локальный путь
        torch_dtype=torch_dtype,
        device=device
    )
    print("[*] Модель Whisper успешно загружена.")
except Exception as e:
    print(f"[-] Ошибка при загрузке модели Whisper: {e}")
    sys.exit(1)

def transcribe_audio_and_save(chat_id, user_file_name, input_mp3_path):
    """
    Распознает речь из MP3-файла, сохраняет текст и отправляет уведомление.
    """
    try:
        if not os.path.exists(input_mp3_path):
            print(f"[-] Ошибка: MP3-файл '{input_mp3_path}' не найден.")
            return False

        # Шаг 1: Распознавание речи напрямую из MP3
        print(f"[+] Chat ID: {chat_id} - Начало транскрибации файла '{user_file_name}'...")
        
        # Передаем путь к MP3-файлу напрямую в pipeline.
        # Библиотека transformers сама использует ffmpeg "под капотом".
        result = pipe(input_mp3_path) 
        text = result['text']
        print(f"[+] Транскрипция для '{user_file_name}' успешно завершена.")

        # Шаг 2: Сохранение текста в файл
        output_dir = os.path.join(UPLOADS_BASE_DIR, str(chat_id), 'txt')
        os.makedirs(output_dir, exist_ok=True)

        safe_user_file_name = "".join([c for c in user_file_name if c.isalnum() or c in (' ', '.', '_')]).rstrip()
        if not safe_user_file_name:
            safe_user_file_name = f"transcription_{chat_id}_{os.path.basename(input_mp3_path)}.txt"
        else:
            safe_user_file_name += ".txt"

        output_text_path = os.path.join(output_dir, safe_user_file_name)
        
        with open(output_text_path, 'w', encoding='utf-8') as f:
            f.write(text)
        
        print(f"[+] Транскрипция сохранена в: '{output_text_path}'")

        # Шаг 3: Отправка сообщения в топик 'done'
        done_message = {
            'chatId': chat_id,
            'userFileName': user_file_name,
            'filePath': output_text_path,
            'status': 'success',
            'message': f"Текст из файла '{user_file_name}' успешно извлечен и сохранен."
        }
        producer.send(DONE_TOPIC_NAME, value=done_message)
        print(f"[+] Уведомление об успехе отправлено в топик '{DONE_TOPIC_NAME}'.")
        return True

    except Exception as e:
        print(f"[-] Произошла ошибка при обработке файла '{input_mp3_path}': {e}")
        error_message = {
            'chatId': chat_id,
            'userFileName': user_file_name,
            'filePath': input_mp3_path,
            'status': 'error',
            'message': f"Ошибка при извлечении текста из файла '{user_file_name}': {e}"
        }
        producer.send(DONE_TOPIC_NAME, value=error_message)
        return False

# --- Основной цикл консьюмера ---
print(f"[*] Ожидаем сообщения в топике '{PROCESS_TOPIC_NAME}'...")

for message in consumer:
    try:
        request_data = message.value
        chat_id = request_data.get('chatID')
        user_file_name = request_data.get('userFileName')
        file_path = request_data.get('filePath')

        if not all([chat_id, user_file_name, file_path]):
            print(f"[-] Получено неполное сообщение: {request_data}")
            continue

        transcribe_audio_and_save(chat_id, user_file_name, file_path)

    except Exception as e:
        print(f"[-] Непредвиденная ошибка в основном цикле: {e}")

