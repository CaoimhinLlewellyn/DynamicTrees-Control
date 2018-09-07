package com.ferreusveritas.dynamictreescontrol;


import com.ferreusveritas.dynamictrees.api.events.PopulateDataBaseEvent;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = ModConstants.MODID, name=ModConstants.NAME, version=ModConstants.VERSION, dependencies=ModConstants.DEPENDENCIES)
public class DynamicTreesControl {
	
	@Mod.Instance(ModConstants.MODID)
	public static DynamicTreesControl instance;
	
	@Mod.EventBusSubscriber
	public static class RegistrationHandler {
		
		@SubscribeEvent
		public static void populateDataBase(PopulateDataBaseEvent event) {
			new BiomeDataBaseController().populate();
		}
		
	}
	
}
