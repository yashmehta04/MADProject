package com.example.madproject.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pre-trained TensorFlow Lite model for mood classification.
 * Uses VGGish and YAMNet models adapted for mobile mood detection.
 */
public class PretrainedMoodModel {
    
    private static final String TAG = "PretrainedMoodModel";
    private static final String MODEL_PATH = "models/vggish_audio_model.tflite";
    private static final String[] MOOD_LABELS = {
        MoodAlgorithm.MOOD_HAPPY,
        MoodAlgorithm.MOOD_SAD,
        MoodAlgorithm.MOOD_CALM,
        MoodAlgorithm.MOOD_ENERGETIC
    };
    
    private Interpreter tflite;
    private Context context;
    private boolean isInitialized = false;
    
    /**
     * Mood prediction result
     */
    public static class MoodPrediction {
        public final String mood;
        public final double confidence;
        public final double[] probabilities;
        
        public MoodPrediction(String mood, double confidence, double[] probabilities) {
            this.mood = mood;
            this.confidence = confidence;
            this.probabilities = probabilities;
        }
    }
    
    public PretrainedMoodModel(Context context) {
        this.context = context;
        initializeModel();
    }
    
    /**
     * Initialize TensorFlow Lite model
     */
    private void initializeModel() {
        try {
            // Load pre-trained model from assets
            MappedByteBuffer modelBuffer = loadModelFile();
            
            // Create interpreter with options
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // Use multiple threads for performance
            options.setUseNNAPI(true); // Use NNAPI for acceleration if available
            
            tflite = new Interpreter(modelBuffer, options);
            isInitialized = true;
            
            Log.d(TAG, "TensorFlow Lite model initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TensorFlow Lite model", e);
            isInitialized = false;
        }
    }
    
    /**
     * Load model file from assets
     */
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    
    /**
     * Predict mood from audio features
     */
    public MoodPrediction predictMood(AudioFeatureExtractor.AudioFeatures features) {
        if (!isInitialized) {
            Log.e(TAG, "Model not initialized");
            return createFallbackPrediction();
        }
        
        try {
            // Prepare input tensor
            float[][] inputTensor = prepareInputTensor(features);
            
            // Prepare output tensor
            float[][] outputTensor = new float[1][MOOD_LABELS.length];
            
            // Run inference
            tflite.run(inputTensor, outputTensor);
            
            // Process results
            return processOutput(outputTensor[0]);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during mood prediction", e);
            return createFallbackPrediction();
        }
    }
    
    /**
     * Prepare input tensor for model
     */
    private float[][] prepareInputTensor(AudioFeatureExtractor.AudioFeatures features) {
        // Convert audio features to model input format
        double[] doubleFeatures = features.toFloatArray();
        float[][] inputTensor = new float[1][doubleFeatures.length];
        
        // Normalize features (important for neural networks)
        for (int i = 0; i < doubleFeatures.length; i++) {
            inputTensor[0][i] = normalizeFeature(doubleFeatures[i], i);
        }
        
        return inputTensor;
    }
    
    /**
     * Normalize individual feature
     */
    private float normalizeFeature(double value, int featureIndex) {
        // Different normalization for different feature types
        switch (featureIndex) {
            case 0: // Tempo (60-200 BPM)
                return (float) ((value - 60) / 140.0);
            case 1: // Energy (0-1)
                return (float) value;
            case 2: // Spectral centroid (0-8000 Hz)
                return (float) (value / 8000.0);
            case 3: // Zero crossing rate (0-1000)
                return (float) (value / 1000.0);
            case 4: // Rhythm regularity (0-1)
                return (float) value;
            case 5: // Harmonic complexity (0-1)
                return (float) value;
            case 6: // Is minor key (0-1)
                return (float) value;
            case 7: // Complex rhythm (0-1)
                return (float) value;
            case 8: // Sitar dominant (0-1)
                return (float) value;
            case 9: // Guitar dominant (0-1)
                return (float) value;
            default: // Frequency bands (0-1)
                return (float) Math.min(1.0, Math.max(0.0, value / 1000.0));
        }
    }
    
    /**
     * Process model output to get mood prediction
     */
    private MoodPrediction processOutput(float[] output) {
        // Apply softmax to get probabilities
        float[] probabilities = applySoftmax(output);
        
        // Find the mood with highest probability
        int maxIndex = 0;
        float maxProb = probabilities[0];
        
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i];
                maxIndex = i;
            }
        }
        
        String predictedMood = MOOD_LABELS[maxIndex];
        double confidence = maxProb;
        
        Log.d(TAG, "Model prediction: " + predictedMood + " (confidence: " + confidence + ")");
        
        return new MoodPrediction(predictedMood, confidence, toDoubleArray(probabilities));
    }
    
    /**
     * Apply softmax function to output
     */
    private float[] applySoftmax(float[] output) {
        float[] exp = new float[output.length];
        float sum = 0.0f;
        
        // Calculate exponential values
        for (int i = 0; i < output.length; i++) {
            exp[i] = (float) Math.exp(output[i]);
            sum += exp[i];
        }
        
        // Normalize to get probabilities
        float[] probabilities = new float[output.length];
        for (int i = 0; i < output.length; i++) {
            probabilities[i] = exp[i] / sum;
        }
        
        return probabilities;
    }
    
    /**
     * Convert float array to double array
     */
    private double[] toDoubleArray(float[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
    
    /**
     * Create fallback prediction when model fails
     */
    private MoodPrediction createFallbackPrediction() {
        // Use simple heuristic as fallback
        return new MoodPrediction(
            MoodAlgorithm.MOOD_CALM,
            0.5,
            new double[]{0.25, 0.25, 0.25, 0.25}
        );
    }
    
    /**
     * Batch predict moods for multiple songs
     */
    public List<MoodPrediction> predictMoods(List<AudioFeatureExtractor.AudioFeatures> featuresList) {
        List<MoodPrediction> predictions = new ArrayList<>();
        
        for (AudioFeatureExtractor.AudioFeatures features : featuresList) {
            predictions.add(predictMood(features));
        }
        
        return predictions;
    }
    
    /**
     * Get model information
     */
    public String getModelInfo() {
        if (!isInitialized) {
            return "Model not initialized";
        }
        
        try {
            // Get input/output shapes
            int[] inputShape = tflite.getInputTensor(0).shape();
            int[] outputShape = tflite.getOutputTensor(0).shape();
            
            return "Model: " + MODEL_PATH + 
                   "\nInput Shape: " + java.util.Arrays.toString(inputShape) +
                   "\nOutput Shape: " + java.util.Arrays.toString(outputShape) +
                   "\nLabels: " + java.util.Arrays.toString(MOOD_LABELS);
                   
        } catch (Exception e) {
            return "Error getting model info: " + e.getMessage();
        }
    }
    
    /**
     * Check if model is initialized and ready
     */
    public boolean isReady() {
        return isInitialized;
    }
    
    /**
     * Close model and release resources
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        isInitialized = false;
        Log.d(TAG, "Model closed and resources released");
    }
    
    /**
     * Get confidence threshold for filtering low-confidence predictions
     */
    public double getConfidenceThreshold() {
        return 0.6; // 60% confidence threshold
    }
    
    /**
     * Check if prediction meets confidence threshold
     */
    public boolean isConfidentPrediction(MoodPrediction prediction) {
        return prediction.confidence >= getConfidenceThreshold();
    }
    
    /**
     * Get top-k predictions
     */
    public List<String> getTopKMoods(AudioFeatureExtractor.AudioFeatures features, int k) {
        MoodPrediction prediction = predictMood(features);
        
        // Sort probabilities by confidence
        java.util.List<java.util.Map.Entry<String, Double>> sorted = new java.util.ArrayList<>();
        for (int i = 0; i < MOOD_LABELS.length; i++) {
            sorted.add(new java.util.AbstractMap.SimpleEntry<>(
                MOOD_LABELS[i], prediction.probabilities[i]
            ));
        }
        
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // Return top-k moods
        List<String> topMoods = new ArrayList<>();
        for (int i = 0; i < Math.min(k, sorted.size()); i++) {
            topMoods.add(sorted.get(i).getKey());
        }
        
        return topMoods;
    }
}
