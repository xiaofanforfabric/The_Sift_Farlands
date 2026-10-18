package mielon.siftfarlands.mixin;

import mielon.siftfarlands.SiftFarlands;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Applies the position gate to the compiled noise sampler.
 *
 * <p>{@link NoiseFunctionMixin} negates {@code xzScale} for The Sift's noises.
 * Here that marker is turned into a real scale: below
 * {@link SiftFarlands#FARLANDS_START} the original (positive) scale is
 * restored, beyond it the forced scale is used. Every other sampler, vanilla
 * dimensions included, keeps its positive scale and is never touched.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.densityfunction.generator.NoiseFunction$Sampler")
public abstract class NoiseFunctionSamplerMixin {

	@Redirect(
			method = "sampleValue",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/world/level/levelgen/densityfunction/generator/NoiseFunction$Sampler;xzScale:D"))
	private static double sift$gateValue(NoiseFunction.Sampler self, SamplerContext context,
										 int x, int y, int z) {
		return SiftFarlands.scaleFor(self.xzScale(), x, z);
	}

	@Redirect(
			method = "sampleVolume",
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
					target = "Lnet/minecraft/world/level/levelgen/densityfunction/generator/NoiseFunction$Sampler;xzScale:D"))
	private static double sift$gateVolume(NoiseFunction.Sampler self, SamplerContext context,
										  DensityBuffer buffer, DensityVolume volume) {
		int extent = Math.max(
				Math.max(Math.abs(volume.minBlockX()), Math.abs(volume.maxBlockX())),
				Math.max(Math.abs(volume.minBlockZ()), Math.abs(volume.maxBlockZ())));
		return SiftFarlands.scaleForExtent(self.xzScale(), extent);
	}
}
