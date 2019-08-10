package physica.missiles;

import java.io.File;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Metadata;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import physica.CoreReferences;
import physica.api.core.abstraction.recipe.IRecipeRegister;
import physica.api.core.load.ContentLoader;
import physica.api.core.load.LoadPhase;
import physica.library.location.VectorLocation;
import physica.missiles.client.MissileClientRegister;
import physica.missiles.common.MissileBlockRegister;
import physica.missiles.common.MissileEntityRegister;
import physica.missiles.common.MissileItemRegister;
import physica.missiles.common.MissileTabRegister;
import physica.missiles.common.config.ConfigMissiles;
import physica.missiles.common.entity.EntityGrenade;
import physica.proxy.CommonProxy;

@Mod(modid = MissileReferences.DOMAIN, name = MissileReferences.NAME, version = CoreReferences.VERSION, dependencies = "required-after:" + CoreReferences.DOMAIN)
public class PhysicaMissiles {

	@SidedProxy(clientSide = "physica.proxy.ClientProxy", serverSide = "physica.proxy.ServerProxy")
	public static CommonProxy		sidedProxy;
	public static ContentLoader		proxyLoader	= new ContentLoader();

	@Instance(MissileReferences.NAME)
	public static PhysicaMissiles	INSTANCE;
	@Metadata(MissileReferences.DOMAIN)
	public static ModMetadata		metadata;

	public static File				configFolder;
	public static ConfigMissiles	config;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		INSTANCE = this;
		configFolder = new File(event.getModConfigurationDirectory(), "/" + MissileReferences.DOMAIN);
		proxyLoader.addContent(sidedProxy);
		proxyLoader.addContent(config = new ConfigMissiles());
		proxyLoader.addContent(new MissileTabRegister());

		// proxyLoader.addContent(new NuclearFluidRegister());
		proxyLoader.addContent(new MissileBlockRegister());
		proxyLoader.addContent(new MissileItemRegister());
		proxyLoader.addContent(new MissileEntityRegister());

		if (event.getSide() == Side.CLIENT)
		{
			proxyLoader.addContent(new MissileClientRegister());
		}

		// proxyLoader.addContent(new NuclearRecipeRegister());
		// proxyLoader.addContent(new NuclearWorldGenRegister());
		metadata.authorList = CoreReferences.Metadata.AUTHORS;
		metadata.autogenerated = false;
		metadata.credits = CoreReferences.Metadata.CREDITS;
		metadata.description = CoreReferences.Metadata.DESCRIPTION.replace("Physica", MissileReferences.NAME);
		metadata.modId = MissileReferences.DOMAIN;
		metadata.name = MissileReferences.NAME;
		metadata.parent = CoreReferences.DOMAIN;
		metadata.updateUrl = CoreReferences.Metadata.UPDATE_URL;
		metadata.url = CoreReferences.Metadata.URL;
		metadata.version = CoreReferences.VERSION;
		proxyLoader.callRegister(LoadPhase.CreativeTabRegister);
		proxyLoader.callRegister(LoadPhase.ConfigRegister);
		proxyLoader.callRegister(LoadPhase.RegisterObjects);
		proxyLoader.callRegister(LoadPhase.PreInitialize);
		proxyLoader.callRegister(LoadPhase.ClientRegister);
		BlockDispenser.dispenseBehaviorRegistry.putObject(MissileItemRegister.itemGrenade, (IBehaviorDispenseItem) (blockSource, itemStack) ->
		{
			World world = blockSource.getWorld();

			if (!world.isRemote)
			{
				int x = blockSource.getXInt();
				int y = blockSource.getYInt();
				int z = blockSource.getZInt();
				EnumFacing enumFacing = EnumFacing.getFront(blockSource.getBlockMetadata());

				EntityGrenade entity = new EntityGrenade(world, new VectorLocation(x, y, z), itemStack.getItemDamage());
				entity.setThrowableHeading(enumFacing.getFrontOffsetX(), 0.1D, enumFacing.getFrontOffsetZ(), 0.5F, 1.0F);
				world.spawnEntityInWorld(entity);
			}

			itemStack.stackSize--;

			return itemStack;
		});
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		proxyLoader.callRegister(LoadPhase.Initialize);
		proxyLoader.callRegister(LoadPhase.EntityRegister);
		proxyLoader.callRegister(LoadPhase.FluidRegister);
		proxyLoader.callRegister(LoadPhase.WorldRegister);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		proxyLoader.callRegister(LoadPhase.PostInitialize);
	}

	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent event)
	{
		proxyLoader.callRegister(LoadPhase.OnStartup);
		IRecipeRegister.callRegister("Missiles");
	}
}
