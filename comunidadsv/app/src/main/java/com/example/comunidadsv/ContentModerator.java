package com.example.comunidadsv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.util.ArrayList;
import java.util.List;

public class ContentModerator {
    private static final String TAG = "ContentModerator";
    private static final float NSFW_THRESHOLD = 0.7f;
    private ImageLabeler imageLabeler;

    // Lista de palabras prohibidas (Español e Inglés)
    private static final String[] BANNED_WORDS = {
            // ESPAÑOL - Groserías básicas
            "puta", "puto", "putas", "putos",
            "verga", "vergas", "verguear",
            "pene", "penes", "vagina", "vaginas",
            "culo", "culos", "coño", "coños", "concha",
            "pajero", "pajera", "mamada", "mamadas",
            "chupar", "chupada", "follar", "follando",
            "coger", "cogiendo", "violar", "violacion", "violador",

            // ESPAÑOL - Insultos
            "idiota", "idiotas", "estupido", "estupida", "estupidos",
            "imbecil", "imbeciles", "tarado", "tarada", "pendejo", "pendeja",
            "gilipollas", "capullo", "cabron", "cabrona", "cabrones",
            "hijueputa", "hijodeputa", "malparido", "gonorrea",
            "marica", "maricon", "bobo", "boba", "tonto", "tonta",

            // ESPAÑOL - Sexuales
            "desnudo", "desnuda", "desnudos", "sexo", "sexual",
            "porno", "pornografia", "xxx", "tetas", "nalgas",
            "orgasmo", "eyacular", "masturbar", "paja",

            // ESPAÑOL - Violencia
            "matar", "muerte", "asesino", "asesinar", "violencia",
            "arma", "pistola", "cuchillo", "bomba", "suicidio",
            "sangre", "golpear", "apuñalar", "secuestro",

            // ESPAÑOL - Discriminación
            "nazi", "racista", "racismo", "homofobico", "homofobia",
            "machista", "discriminar", "negro", "indio", "judio",

            // ESPAÑOL - Drogas
            "droga", "drogas", "cocaina", "marihuana", "heroina",
            "extasis", "anfetamina", "traficante",

            // ESPAÑOL - Otras
            "mierda", "caca", "pedo", "pis", "asqueroso", "chingar",
            "joder", "jodido", "carajo", "cojones", "huevada",

            // ENGLISH - Profanity
            "fuck", "fucking", "fucked", "fucker", "motherfucker",
            "shit", "shitting", "bullshit", "shitty",
            "damn", "goddamn", "hell",
            "ass", "asshole", "asses",
            "bitch", "bitches", "bastard", "bastards",
            "dick", "dicks", "dickhead", "cock", "cocks",
            "pussy", "pussies", "cunt", "cunts",
            "whore", "whores", "slut", "sluts",

            // ENGLISH - Sexual
            "porn", "porno", "pornography", "nsfw",
            "nude", "naked", "nudes", "nudity",
            "sex", "sexy", "penis", "vagina", "breast", "boobs", "tits",
            "blowjob", "handjob", "orgasm", "ejaculate",
            "incest", "pedophilia", "rape", "raped",

            // ENGLISH - Violence
            "kill", "killing", "murder", "murderer", "death",
            "die", "dying", "dead", "suicide", "kys",
            "weapon", "gun", "pistol", "knife", "bomb",
            "blood", "bloody", "gore", "torture",
            "terrorist", "terrorism", "hate",

            // ENGLISH - Discrimination
            "nazi", "fascist", "racist", "racism",
            "nigger", "nigga", "faggot", "tranny",
            "homophobic", "sexist", "misogynist",

            // ENGLISH - Drugs
            "drug", "drugs", "cocaine", "coke", "heroin",
            "meth", "crystal", "ecstasy", "mdma",
            "marijuana", "weed", "cannabis", "opium",

            // ENGLISH - Insults
            "stupid", "dumb", "idiot", "moron", "retard",
            "loser", "worthless", "pathetic", "jerk", "douchebag"
    };

    public ContentModerator(Context context) {
        try {
            ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(0.7f)
                    .build();
            imageLabeler = ImageLabeling.getClient(options);
            Log.d(TAG, "ContentModerator inicializado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando ImageLabeler", e);
        }
    }

    /**
     * Analiza una imagen para detectar contenido inapropiado
     */
    public void analyzeImage(Bitmap bitmap, ModerationCallback callback) {
        if (bitmap == null) {
            if (callback != null) callback.onApproved();
            return;
        }

        if (imageLabeler == null) {
            Log.e(TAG, "ImageLabeler no inicializado");
            if (callback != null) callback.onApproved();
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        imageLabeler.process(image)
                .addOnSuccessListener(labels -> {
                    boolean isInappropriate = false;
                    String detectedLabel = "";

                    for (ImageLabel label : labels) {
                        String text = label.getText().toLowerCase();
                        float confidence = label.getConfidence();

                        Log.d(TAG, "Etiqueta detectada: " + text + " (" + confidence + ")");

                        if (isNsfwLabel(text, confidence)) {
                            isInappropriate = true;
                            detectedLabel = text;
                            break;
                        }
                    }

                    if (callback != null) {
                        if (isInappropriate) {
                            callback.onRejected("imagen inapropiada: " + detectedLabel);
                        } else {
                            callback.onApproved();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error analizando imagen", e);
                    if (callback != null) callback.onApproved();
                });
    }

    /**
     * Analiza texto para detectar contenido ofensivo
     */
    public ModerationResult analyzeText(String text) {
        if (text == null || text.isEmpty()) {
            return new ModerationResult(true, "");
        }

        String lowerText = text.toLowerCase();
        List<String> foundIssues = new ArrayList<>();

        for (String word : BANNED_WORDS) {
            if (lowerText.contains(word)) {
                foundIssues.add(word);
                break; // Solo mostrar la primera palabra encontrada
            }
        }

        if (!foundIssues.isEmpty()) {
            return new ModerationResult(false, "palabra prohibida: " + foundIssues.get(0));
        }

        return new ModerationResult(true, "");
    }

    private boolean isNsfwLabel(String label, float confidence) {
        if (confidence < NSFW_THRESHOLD) return false;

        String[] nsfwLabels = {
                "breast", "nipple", "buttocks", "genital", "porn", "nsfw",
                "underwear", "lingerie", "bikini", "swimsuit", "bathing suit",
                "sexual activity", "erotic", "adult content", "lingerie"
        };

        for (String nsfw : nsfwLabels) {
            if (label.contains(nsfw)) {
                return true;
            }
        }
        return false;
    }

    // Interfaz para callback de moderación de imágenes
    public interface ModerationCallback {
        void onApproved();
        void onRejected(String reason);
    }

    // Clase para resultados de moderación de texto
    public static class ModerationResult {
        public boolean isAppropriate;
        public String reason;

        public ModerationResult(boolean isAppropriate, String reason) {
            this.isAppropriate = isAppropriate;
            this.reason = reason;
        }
    }
}