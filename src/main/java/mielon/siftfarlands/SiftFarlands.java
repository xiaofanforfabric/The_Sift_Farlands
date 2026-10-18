package mielon.siftfarlands;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Sift Farlands.
 *
 * <p>The mod does exactly one thing: it removes the Beta 1.8 modulo patch from
 * {@code GradientNoise.wrap}, so the Perlin integer cast can overflow again.
 * Where Far Lands appear is not decided here - it follows from the overflow
 * itself, exactly as it did before Beta 1.8.
 */
public final class SiftFarlands implements ModInitializer {
	public static final String MOD_ID = "sift_farlands";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Overflow of the {@code (int)} cast starts at 2^31 noise units. */
	public static final long OVERFLOW_POINT = 1L << 31;

	/** Largest {@code |input|} handed to {@code wrap}, for diagnostics. */
	private static final AtomicLong MAX_INPUT = new AtomicLong();

	/**
	 * {@code xz_scale} fed to every noise sampler.
	 *
	 * <p>{@code NoiseFunction} passes {@code x * xzScale} to {@code NoiseStack},
	 * which multiplies again by each octave's frequency
	 * ({@code 2^octave}). The Sift's steepest octave is {@code 2^-4}: the live
	 * diagnostic measured 43.55 units/block at 12,550,820 blocks with xzScale
	 * 684.412, i.e. {@code 684.412 * 0.0625}. Reaching the vanilla
	 * 171.103 units/block therefore needs {@code 171.103 / 0.0625 = 2737.648}.
	 */
	public static final double NOISE_SCALE = 2737.648;

	/**
	 * Block coordinate at which the forced scale switches on. Below this the
	 * sampler keeps the biome's own {@code xz_scale}, so the world stays normal.
	 */
	public static final int FARLANDS_START = 3_081_840;

	/**
	 * Resolves the scale to use for one sample.
	 *
	 * <p>{@link mielon.siftfarlands.mixin.NoiseFunctionMixin} pre-negates the
	 * {@code xz_scale} of every Sift noise, so a negative value here means
	 * "this sampler belongs to The Sift". The original (positive) scale is
	 * returned below {@link #FARLANDS_START}; the forced scale is returned
	 * beyond it.
	 */
	public static double scaleFor(double rawScale, int blockX, int blockZ) {
		return scaleForExtent(rawScale, Math.max(Math.abs(blockX), Math.abs(blockZ)));
	}

	/** Volume variant of {@link #scaleFor}: pass the largest |x| or |z| in it. */
	public static double scaleForExtent(double rawScale, int maxAbsCoord) {
		if (rawScale >= 0.0) {
			return rawScale;
		}
		return maxAbsCoord >= FARLANDS_START ? NOISE_SCALE : -rawScale;
	}

	/** Vanilla far-lands noise advance per block, for reference. */
	public static final double VANILLA_UNITS_PER_BLOCK = 171.103;

	/** Cutoff before the modulo patch would have folded the value. */
	public static final long WRAP_FOLD_POINT = 1L << 24;

	private int ticks;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++ticks % 200 != 0) {
				return;
			}
			long max = MAX_INPUT.getAndSet(0L);
			LOGGER.info("[diag] max noise input = {} ({}.{}% of 2^31, wrap fold = {})",
					max,
					max * 100L / OVERFLOW_POINT,
					(max % 100L),
					max >= WRAP_FOLD_POINT);
		});
		LOGGER.info("Sift Farlands initialized (modulo patch removed).");
	}

	/** Called from the {@code wrap} hook for every noise sample. */
	public static void recordInput(double value) {
		double magnitude = Math.abs(value);
		if (magnitude > MAX_INPUT.get()) {
			MAX_INPUT.set((long) magnitude);
		}
	}
}
