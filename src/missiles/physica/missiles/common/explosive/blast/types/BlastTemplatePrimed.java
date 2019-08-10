package physica.missiles.common.explosive.blast.types;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import physica.library.location.Location;
import physica.missiles.common.explosive.blast.BlastTemplate;
import physica.missiles.common.explosive.blast.IStateHolder;

public class BlastTemplatePrimed extends BlastTemplate {
	public final float size;

	public BlastTemplatePrimed(int fuseTime, int tier, int callCount, float size) {
		super(fuseTime, tier, callCount);
		this.size = size;
	}

	@Override
	public void prepare(World world, Location loc, IStateHolder holder)
	{
		world.spawnParticle("hugeexplosion", loc.xCoord + 0.5, loc.yCoord + 0.5, loc.zCoord + 0.5, 0.0D, 0.0D, 0.0D);
	}

	@Override
	public void call(World world, Location loc, int callCount, IStateHolder holder)
	{
		if (!world.isRemote)
		{
			world.createExplosion((Entity) holder.getObject("cause"), loc.xCoord + 0.5, loc.yCoord + 0.5, loc.zCoord + 0.5, size, true);
		}
	}
}
