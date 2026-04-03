package com.example.madproject.utils;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced audio feature extraction for mood analysis.
 * Extracts comprehensive audio fingerprint including tempo, energy, frequency analysis.
 */
public class AudioFeatureExtractor {
    
    private static final String TAG = "AudioFeatureExtractor";
    private static final int SAMPLE_RATE = 44100;
    private static final int WINDOW_SIZE = 1024;
    private static final int HOP_SIZE = 512;
    
    /**
     * Comprehensive audio features data class
     */
    public static class AudioFeatures {
        public final double tempo;              // BPM
        public final double energy;             // RMS amplitude
        public final double spectralCentroid;   // Brightness
        public final double zeroCrossingRate;   // Roughness
        public final double[] frequencyBands;   // Bass/Mid/Treble distribution
        public final double rhythmRegularity;   // Beat consistency
        public final double harmonicComplexity; // Chord changes
        public final boolean isMinorKey;        // Emotional coloring
        public final boolean complexRhythm;     // Cultural indicator
        public final boolean sitarDominant;     // Cultural indicator
        public final boolean guitarDominant;    // Cultural indicator
        
        public AudioFeatures(double tempo, double energy, double spectralCentroid, 
                           double zeroCrossingRate, double[] frequencyBands, 
                           double rhythmRegularity, double harmonicComplexity, 
                           boolean isMinorKey, boolean complexRhythm, 
                           boolean sitarDominant, boolean guitarDominant) {
            this.tempo = tempo;
            this.energy = energy;
            this.spectralCentroid = spectralCentroid;
            this.zeroCrossingRate = zeroCrossingRate;
            this.frequencyBands = frequencyBands;
            this.rhythmRegularity = rhythmRegularity;
            this.harmonicComplexity = harmonicComplexity;
            this.isMinorKey = isMinorKey;
            this.complexRhythm = complexRhythm;
            this.sitarDominant = sitarDominant;
            this.guitarDominant = guitarDominant;
        }
        
        public double[] toFloatArray() {
            double[] features = new double[128];
            features[0] = tempo;
            features[1] = energy;
            features[2] = spectralCentroid;
            features[3] = zeroCrossingRate;
            features[4] = rhythmRegularity;
            features[5] = harmonicComplexity;
            features[6] = isMinorKey ? 1.0 : 0.0;
            features[7] = complexRhythm ? 1.0 : 0.0;
            features[8] = sitarDominant ? 1.0 : 0.0;
            features[9] = guitarDominant ? 1.0 : 0.0;
            
            // Add frequency bands (20 dimensions)
            System.arraycopy(frequencyBands, 0, features, 10, Math.min(frequencyBands.length, 118));
            
            return features;
        }
    }
    
    /**
     * Extract comprehensive audio features from song file
     */
    public AudioFeatures extractFeatures(String songPath) {
        try {
            // Extract PCM audio data
            short[] audioData = extractAudioData(songPath);
            
            if (audioData == null || audioData.length == 0) {
                return createDefaultFeatures();
            }
            
            // Perform comprehensive analysis
            double tempo = extractTempo(audioData);
            double energy = calculateEnergy(audioData);
            double spectralCentroid = calculateSpectralCentroid(audioData);
            double zeroCrossingRate = calculateZeroCrossingRate(audioData);
            double[] frequencyBands = extractFrequencyBands(audioData);
            double rhythmRegularity = analyzeRhythmRegularity(audioData);
            double harmonicComplexity = analyzeHarmonicComplexity(audioData);
            boolean isMinorKey = detectKeyMode(audioData);
            boolean complexRhythm = detectComplexRhythm(audioData, tempo);
            boolean sitarDominant = detectSitarPresence(frequencyBands);
            boolean guitarDominant = detectGuitarPresence(frequencyBands);
            
            return new AudioFeatures(
                tempo, energy, spectralCentroid, zeroCrossingRate, frequencyBands,
                rhythmRegularity, harmonicComplexity, isMinorKey, complexRhythm,
                sitarDominant, guitarDominant
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting audio features", e);
            return createDefaultFeatures();
        }
    }
    
    /**
     * Extract raw PCM audio data from audio file
     */
    private short[] extractAudioData(String songPath) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(songPath);
            
            // Find audio track
            int audioTrackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }
            
            if (audioTrackIndex == -1) {
                return null;
            }
            
            extractor.selectTrack(audioTrackIndex);
            MediaFormat format = extractor.getTrackFormat(audioTrackIndex);
            
            // Extract audio samples
            List<Short> samples = new ArrayList<>();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 4);
            
            while (true) {
                int sampleSize = extractor.readSampleData(byteBuffer, 0);
                if (sampleSize <= 0) break;
                
                extractor.advance();
                
                // Convert to PCM samples (simplified for demo)
                for (int i = 0; i < sampleSize - 1; i += 2) {
                    short sample = byteBuffer.getShort(i);
                    samples.add(sample);
                }
            }
            
            extractor.release();
            
            // Convert list to array
            short[] audioData = new short[samples.size()];
            for (int i = 0; i < samples.size(); i++) {
                audioData[i] = samples.get(i);
            }
            
            return audioData;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting audio data", e);
            return null;
        }
    }
    
    /**
     * Extract tempo (BPM) using onset detection
     */
    private double extractTempo(short[] audioData) {
        // Simplified tempo detection - in reality would use autocorrelation or FFT
        double[] onsets = detectOnsets(audioData);
        double[] intervals = calculateIntervals(onsets);
        
        if (intervals.length == 0) return 120.0; // Default tempo
        
        // Find most common interval
        double avgInterval = 0;
        for (double interval : intervals) {
            avgInterval += interval;
        }
        avgInterval /= intervals.length;
        
        // Convert interval to BPM
        double bpm = 60.0 / avgInterval;
        
        // Clamp to realistic range
        return Math.max(60, Math.min(200, bpm));
    }
    
    /**
     * Calculate RMS energy level
     */
    private double calculateEnergy(short[] audioData) {
        double sum = 0;
        for (short sample : audioData) {
            sum += (sample * sample);
        }
        return Math.sqrt(sum / audioData.length) / Short.MAX_VALUE;
    }
    
    /**
     * Calculate spectral centroid (brightness indicator)
     */
    private double calculateSpectralCentroid(short[] audioData) {
        // Simplified spectral centroid calculation
        // In reality would use FFT
        double centroid = 0;
        double magnitude = 0;
        
        for (int i = 0; i < audioData.length; i++) {
            double freq = i * SAMPLE_RATE / audioData.length;
            double amp = Math.abs(audioData[i]) / Short.MAX_VALUE;
            centroid += freq * amp;
            magnitude += amp;
        }
        
        return magnitude > 0 ? centroid / magnitude : 1000;
    }
    
    /**
     * Calculate zero crossing rate (roughness indicator)
     */
    private double calculateZeroCrossingRate(short[] audioData) {
        int crossings = 0;
        for (int i = 1; i < audioData.length; i++) {
            if ((audioData[i-1] >= 0 && audioData[i] < 0) || 
                (audioData[i-1] < 0 && audioData[i] >= 0)) {
                crossings++;
            }
        }
        return (double) crossings / audioData.length * SAMPLE_RATE;
    }
    
    /**
     * Extract frequency bands distribution
     */
    private double[] extractFrequencyBands(short[] audioData) {
        double[] bands = new double[20]; // 20 frequency bands
        
        // Simplified frequency analysis
        // In reality would use FFT with proper frequency bins
        int bandSize = audioData.length / 20;
        
        for (int band = 0; band < 20; band++) {
            double energy = 0;
            int start = band * bandSize;
            int end = Math.min(start + bandSize, audioData.length);
            
            for (int i = start; i < end; i++) {
                energy += Math.abs(audioData[i]);
            }
            
            bands[band] = energy / (end - start);
        }
        
        return bands;
    }
    
    /**
     * Analyze rhythm regularity
     */
    private double analyzeRhythmRegularity(short[] audioData) {
        // Simplified rhythm analysis
        // In reality would use beat tracking algorithms
        double[] onsets = detectOnsets(audioData);
        
        if (onsets.length < 2) return 0.5;
        
        double variance = 0;
        double mean = 0;
        
        // Calculate intervals
        for (int i = 1; i < onsets.length; i++) {
            double interval = onsets[i] - onsets[i-1];
            mean += interval;
        }
        mean /= (onsets.length - 1);
        
        // Calculate variance
        for (int i = 1; i < onsets.length; i++) {
            double interval = onsets[i] - onsets[i-1];
            variance += Math.pow(interval - mean, 2);
        }
        variance /= (onsets.length - 1);
        
        // Return regularity (inverse of variance)
        return 1.0 / (1.0 + variance);
    }
    
    /**
     * Analyze harmonic complexity
     */
    private double analyzeHarmonicComplexity(short[] audioData) {
        // Simplified harmonic analysis
        // In reality would use chord detection algorithms
        double complexity = 0;
        
        // Look for frequency changes that indicate chord changes
        int windowSize = 1024;
        double prevSpectralCentroid = 0;
        
        for (int i = 0; i < audioData.length - windowSize; i += windowSize / 2) {
            short[] window = new short[windowSize];
            System.arraycopy(audioData, i, window, 0, windowSize);
            
            double centroid = calculateSpectralCentroid(window);
            double change = Math.abs(centroid - prevSpectralCentroid);
            complexity += change;
            prevSpectralCentroid = centroid;
        }
        
        return complexity / (audioData.length / (windowSize / 2));
    }
    
    /**
     * Detect key mode (major/minor)
     */
    private boolean detectKeyMode(short[] audioData) {
        // Simplified key detection
        // In reality would use pitch class profiles
        double minorIndicator = 0;
        double majorIndicator = 0;
        
        // Look for minor third intervals vs major third intervals
        for (int i = 0; i < audioData.length - 3; i += 3) {
            double interval1 = Math.abs(audioData[i + 1] - audioData[i]);
            double interval2 = Math.abs(audioData[i + 2] - audioData[i + 1]);
            
            // Simplified interval analysis
            if (interval2 > interval1 * 1.2) {
                minorIndicator++;
            } else {
                majorIndicator++;
            }
        }
        
        return minorIndicator > majorIndicator;
    }
    
    /**
     * Detect complex rhythm patterns (cultural indicator)
     */
    private boolean detectComplexRhythm(short[] audioData, double tempo) {
        // Bollywood music often has complex 6/8, 7/8 time signatures
        // Western music is typically 4/4
        
        double[] onsets = detectOnsets(audioData);
        if (onsets.length < 4) return false;
        
        // Analyze rhythm pattern complexity
        double[] intervals = calculateIntervals(onsets);
        double variance = 0;
        double mean = 0;
        
        for (double interval : intervals) {
            mean += interval;
        }
        mean /= intervals.length;
        
        for (double interval : intervals) {
            variance += Math.pow(interval - mean, 2);
        }
        variance /= intervals.length;
        
        // High variance indicates complex rhythm
        return variance > 0.1;
    }
    
    /**
     * Detect sitar presence (Bollywood indicator)
     */
    private boolean detectSitarPresence(double[] frequencyBands) {
        // Sitar has distinctive frequency characteristics
        // Look for energy in specific frequency ranges
        
        // Sitar typically has strong harmonics in mid-high frequencies
        double midEnergy = 0;
        double highEnergy = 0;
        
        // Mid frequencies (bands 5-10)
        for (int i = 5; i < 10; i++) {
            midEnergy += frequencyBands[i];
        }
        
        // High frequencies (bands 10-15)
        for (int i = 10; i < 15; i++) {
            highEnergy += frequencyBands[i];
        }
        
        // Sitar characteristic: strong harmonics
        return highEnergy > midEnergy * 1.5;
    }
    
    /**
     * Detect guitar presence (Western indicator)
     */
    private boolean detectGuitarPresence(double[] frequencyBands) {
        // Guitar has different frequency characteristics than sitar
        
        double lowEnergy = 0;
        double midEnergy = 0;
        
        // Low frequencies (bands 0-5)
        for (int i = 0; i < 5; i++) {
            lowEnergy += frequencyBands[i];
        }
        
        // Mid frequencies (bands 5-10)
        for (int i = 5; i < 10; i++) {
            midEnergy += frequencyBands[i];
        }
        
        // Guitar characteristic: balanced low-mid frequencies
        return Math.abs(lowEnergy - midEnergy) < (lowEnergy + midEnergy) * 0.3;
    }
    
    /**
     * Detect onsets (beat beginnings)
     */
    private double[] detectOnsets(short[] audioData) {
        List<Double> onsets = new ArrayList<>();
        
        // Simplified onset detection using energy changes
        int windowSize = 512;
        double prevEnergy = 0;
        
        for (int i = 0; i < audioData.length - windowSize; i += windowSize / 2) {
            double energy = 0;
            for (int j = i; j < i + windowSize; j++) {
                energy += Math.abs(audioData[j]);
            }
            energy /= windowSize;
            
            // Onset detected if energy increases significantly
            if (energy > prevEnergy * 1.5 && prevEnergy > 0) {
                onsets.add((double) i / SAMPLE_RATE);
            }
            
            prevEnergy = energy;
        }
        
        double[] result = new double[onsets.size()];
        for (int i = 0; i < onsets.size(); i++) {
            result[i] = onsets.get(i);
        }
        
        return result;
    }
    
    /**
     * Calculate intervals between onsets
     */
    private double[] calculateIntervals(double[] onsets) {
        if (onsets.length < 2) return new double[0];
        
        double[] intervals = new double[onsets.length - 1];
        for (int i = 1; i < onsets.length; i++) {
            intervals[i - 1] = onsets[i] - onsets[i - 1];
        }
        
        return intervals;
    }
    
    /**
     * Create default features for fallback
     */
    private AudioFeatures createDefaultFeatures() {
        double[] defaultBands = new double[20];
        for (int i = 0; i < 20; i++) {
            defaultBands[i] = 0.5;
        }
        
        return new AudioFeatures(
            120.0,  // Default tempo
            0.5,    // Default energy
            1000.0, // Default spectral centroid
            0.1,    // Default zero crossing rate
            defaultBands,
            0.5,    // Default rhythm regularity
            0.5,    // Default harmonic complexity
            false,  // Default major key
            false,  // Default simple rhythm
            false,  // Default no sitar
            false   // Default no guitar
        );
    }
}
