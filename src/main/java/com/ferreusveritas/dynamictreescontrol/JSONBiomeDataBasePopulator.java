package com.ferreusveritas.dynamictreescontrol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.ferreusveritas.dynamictrees.api.TreeRegistry;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.EnumChance;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.IChanceSelector;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.IDensitySelector;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.ISpeciesSelector;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.RandomSpeciesSelector;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.SpeciesSelection;
import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors.StaticSpeciesSelector;
import com.ferreusveritas.dynamictrees.api.worldgen.IBiomeDataBasePopulator;
import com.ferreusveritas.dynamictrees.trees.Species;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDataBase;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDataBase.Operation;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

public class JSONBiomeDataBasePopulator implements IBiomeDataBasePopulator {

	private final String DEFAULT = "...";
	private JsonElement jsonElement;
	
	public JSONBiomeDataBasePopulator(JsonElement jsonElement) {
		this.jsonElement = jsonElement;
	}
	
	@Override
	public void populate(BiomeDataBase biomeDataBase) {
		if(jsonElement.isJsonArray()) {
			for(JsonElement sectionElement : jsonElement.getAsJsonArray()) {
				if(sectionElement.isJsonObject()) {
					JsonObject section = sectionElement.getAsJsonObject();
					readSection(section, biomeDataBase);
				}
			}
		}
	}
	
	private void readSection(JsonObject section, BiomeDataBase dbase) {
		System.out.println("  section");
		
		Biome biome = null;
		String biomeName = "";
		
		JsonElement biomeElement = section.get("biome");
		if(biomeElement != null && biomeElement.isJsonPrimitive()) {
			JsonPrimitive primitive = biomeElement.getAsJsonPrimitive();
			if(primitive.isString()) {
				biomeName = primitive.getAsString();
			}
		}
		
		if(!biomeName.isEmpty()) {
			biome = Biome.REGISTRY.getObject(new ResourceLocation(biomeName));
		}
		
		if(biome != null) {
			
			for(Entry<String, JsonElement> entry : section.entrySet()) {
				String entryName = entry.getKey();
				JsonElement element = entry.getValue();
				
				if("species".equals(entryName)) {
					if(element.isJsonObject()) {
						JsonObject object = element.getAsJsonObject();
						Operation operation = readMethod(object);
						ISpeciesSelector speciesSelector = readSpeciesSelector(object);
						if(speciesSelector != null) {
							dbase.setSpeciesSelector(biome, speciesSelector, operation);
						}
					}
				}
				else if("density".equals(entryName)) {
					if(element.isJsonObject()) {
						JsonObject object = element.getAsJsonObject();
						Operation operation = readMethod(object);
						IDensitySelector densitySelector = readDensitySelector(object);
						if(densitySelector != null) {
							dbase.setDensitySelector(biome, densitySelector, operation);
						}
					}
				}
				else if("chance".equals(entryName)) {
					if(element.isJsonObject()) {
						JsonObject object = element.getAsJsonObject();
						Operation operation = readMethod(object);
						IChanceSelector chanceSelector = readChanceSelector(object);
						if(chanceSelector != null) {
							dbase.setChanceSelector(biome, chanceSelector, operation);
						}
					}
				}
			}
		}
	}
	


	private Operation readMethod(JsonObject object) {
		JsonElement method = object.get("method");
		if(method.isJsonPrimitive() && method.getAsJsonPrimitive().isString()) {
			String methodName = method.getAsJsonPrimitive().getAsString();
			
			if("replace".equals(methodName)) {
				return Operation.REPLACE;
			}
			if("before".equals(methodName)) {
				return Operation.SPLICE_BEFORE;
			}
			if("after".equals(methodName)) {
				return Operation.SPLICE_AFTER;
			}
		}
		
		return Operation.REPLACE;
	}
	
	private ISpeciesSelector readSpeciesSelector(JsonObject mainObject) {
		
		JsonElement randomElement = mainObject.get("random");
		if(randomElement != null && randomElement.isJsonObject()) {
			RandomSpeciesSelector rand = new RandomSpeciesSelector();
			for(Entry<String, JsonElement> entry : randomElement.getAsJsonObject().entrySet()) {
				String speciesName = entry.getKey();
				JsonElement speciesElement = entry.getValue();
				int weight = 0;
				if(speciesElement.isJsonPrimitive() && speciesElement.getAsJsonPrimitive().isNumber()) {
					weight = speciesElement.getAsJsonPrimitive().getAsInt();
					if(weight > 0) {
						if(DEFAULT.equals(speciesName)) {
							rand.add(weight);
						} else {
							Species species = TreeRegistry.findSpeciesSloppy(speciesName);
							if(species != Species.NULLSPECIES) {
								rand.add(species, weight);
							}
						}
					}
				}
			}
			
			if(rand.getSize() > 0) {
				return rand;
			}
		}
		
		JsonElement staticElement = mainObject.get("static");
		if(staticElement != null && staticElement.isJsonPrimitive() && staticElement.getAsJsonPrimitive().isString()) {
			String speciesName = staticElement.getAsJsonPrimitive().getAsString();
			if(DEFAULT.equals(speciesName)) {
				return new StaticSpeciesSelector();
			}
			Species species = TreeRegistry.findSpeciesSloppy(speciesName);
			if(species != Species.NULLSPECIES) {
				return new StaticSpeciesSelector(new SpeciesSelection(species));
			}
		}

		return null;
	}
	
	private IChanceSelector readChanceSelector(JsonObject mainObject) {
		
		JsonElement staticElement = mainObject.get("static");
		if(staticElement != null && staticElement.isJsonPrimitive()) {
			if(staticElement.getAsJsonPrimitive().isNumber()) {
				float value = staticElement.getAsJsonPrimitive().getAsFloat();
				if(value <= 0) {
					return (rnd, spc, rad) -> EnumChance.CANCEL;
				}
				if(value >= 1) {
					return (rnd, spc, rad) -> EnumChance.OK;
				}
				return (rnd, spc, rad) -> rnd.nextFloat() < value ? EnumChance.OK : EnumChance.CANCEL;
			}
			if(staticElement.getAsJsonPrimitive().isString()) {
				String value = staticElement.getAsString();
				if(DEFAULT.equals(value)) {
					return (rnd, spc, rad) -> EnumChance.UNHANDLED;
				}
			}
		}
		
		return null;
	}
	
	private IDensitySelector readDensitySelector(JsonObject mainObject) {
		
		JsonElement scaleElement = mainObject.get("scale");
		if(scaleElement != null && scaleElement.isJsonArray()) {
			List<Float> parameters = new ArrayList<>();
			for(JsonElement element : scaleElement.getAsJsonArray()) {
				if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
					parameters.add(element.getAsJsonPrimitive().getAsFloat());
				}
			}
			switch(parameters.size()) {
				case 0: return (rnd, n) -> n;
				case 1: return (rnd, n) -> n * parameters.get(0);
				case 2: return (rnd, n) -> (n * parameters.get(0)) + parameters.get(1);
				case 3: return (rnd, n) -> ((n * parameters.get(0)) + parameters.get(1)) * parameters.get(2);
				default: return (rnd, n) -> 0.0f;
			}
		}
		
		JsonElement staticElement = mainObject.get("static");
		if(staticElement != null && staticElement.isJsonPrimitive() && staticElement.getAsJsonPrimitive().isNumber()) {
			return (rnd, n) -> staticElement.getAsJsonPrimitive().getAsFloat();
		}
		
		return null;
	}
		
}
