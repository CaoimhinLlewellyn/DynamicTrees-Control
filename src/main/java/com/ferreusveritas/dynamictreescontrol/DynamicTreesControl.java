package com.ferreusveritas.dynamictreescontrol;


import java.io.File;

import com.ferreusveritas.dynamictrees.api.events.PopulateDataBaseEvent;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = ModConstants.MODID, name=ModConstants.NAME, version=ModConstants.VERSION, dependencies=ModConstants.DEPENDENCIES)
public class DynamicTreesControl {
	
	@Mod.Instance(ModConstants.MODID)
	public static DynamicTreesControl instance;
	
	public static File configDirectory;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		configDirectory = event.getModConfigurationDirectory();
	}
	
	@Mod.EventBusSubscriber
	public static class RegistrationHandler {
		
		@SubscribeEvent
		public static void populateDataBase(PopulateDataBaseEvent event) {
			new BiomeDataBaseController(configDirectory).populate();
		}
		
	}
	
}
