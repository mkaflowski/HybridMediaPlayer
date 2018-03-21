package hybridmediaplayer;

import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;

import java.util.ArrayList;
import java.util.List;

public class SeekableExtractorsFactory implements ExtractorsFactory {
    // Lazily initialized default extractor classes in priority order.
    private static List<Class<? extends Extractor>> defaultExtractorClasses;

    /**
     * Creates a new factory for the default extractors.
     */
    public SeekableExtractorsFactory() {
        synchronized (DefaultExtractorsFactory.class) {
            if (defaultExtractorClasses == null) {
                // Lazily initialize defaultExtractorClasses.
                List<Class<? extends Extractor>> extractorClasses = new ArrayList<>();
                // We reference extractors using reflection so that they can be deleted cleanly.
                // Class.forName is used so that automated tools like proguard can detect the use of
                // reflection (see http://proguard.sourceforge.net/FAQ.html#forname).
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.mkv.MatroskaExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.mp4.FragmentedMp4Extractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.mp4.Mp4Extractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.mp3.Mp3Extractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.ts.AdtsExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.ts.Ac3Extractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.ts.TsExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.flv.FlvExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.ogg.OggExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.ts.PsExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.extractor.wav.WavExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                try {
                    extractorClasses.add(
                            Class.forName("com.google.android.exoplayer2.ext.flac.FlacExtractor")
                                    .asSubclass(Extractor.class));
                } catch (ClassNotFoundException e) {
                    // Extractor not found.
                }
                defaultExtractorClasses = extractorClasses;
            }
        }
    }

    @Override
    public Extractor[] createExtractors() {
        Extractor[] extractors = new Extractor[defaultExtractorClasses.size()];
        for (int i = 0; i < extractors.length; i++) {
            try {
                extractors[i] = defaultExtractorClasses.get(i).getConstructor().newInstance();
                if (extractors[i] instanceof Mp3Extractor)
                    extractors[i] = new Mp3Extractor(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING);
            } catch (Exception e) {
                // Should never happen.
                throw new IllegalStateException("Unexpected error creating default extractor", e);
            }
        }
        return extractors;
    }
}
