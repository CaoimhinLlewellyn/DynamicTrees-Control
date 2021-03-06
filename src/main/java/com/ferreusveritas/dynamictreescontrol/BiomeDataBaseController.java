package com.ferreusveritas.dynamictreescontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ferreusveritas.dynamictrees.api.WorldGenRegistry;
import com.ferreusveritas.dynamictrees.api.worldgen.IBiomeDataBasePopulator;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDataBase;
import com.ferreusveritas.dynamictrees.worldgen.TreeGenerator;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class BiomeDataBaseController {

	private Map<String, BiomeDataBaseMultiPopulator> dBaseMap = new HashMap<>();
	private Map<Integer, String> dimensionMap = new HashMap<>();
	private Map<String, IBiomeDataBasePopulator> populatorMap = new HashMap<>();
	private final String DEFAULT = "...";
	
	private File configDirectory;
	
	public BiomeDataBaseController(File configDirectory) {
		this.configDirectory = configDirectory;
	}
	
	public void populate() {
		processFromJsonConfigFile(new File(configDirectory.getAbsolutePath() + "/dynamictreescontrol.json"));
		
		//processFromJsonResource(new ResourceLocation(ModConstants.MODID, "control/example.json")); //TODO:  File name and location are bogus
	}
	
	private JsonObject getJsonFromConfigFile(File file) {
		
        if (file != null && file.exists() && file.isFile() && file.canRead()) {
            String fileName = file.getAbsolutePath();

            try {
                JsonParser parser = new JsonParser();
                JsonElement je = parser.parse(new FileReader(file));
                return je.getAsJsonObject();
            }
            catch (Exception e) {
                System.err.println("Can't open " + fileName + ": " + e.getMessage());
            }
        }

        return null;
	}
	
	private JsonObject getJsonObjectFromResource(ResourceLocation location) {
		try {
			InputStream in;
			in = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			Gson gson = new Gson();
			JsonElement je = gson.fromJson(reader, JsonElement.class);
			return je.getAsJsonObject();
		}
		catch (Exception e) {
            System.err.println("Can't open " + location.toString() + ": " + e.getMessage());
		}
		
		return null;
	}
	
	public void processFromJsonConfigFile(File file) {
		JsonObject json = getJsonFromConfigFile(file);
		readJsonConfig(json);
		linkData();
	}
	
	public void processFromJsonResource(ResourceLocation location) {
		JsonObject json = getJsonObjectFromResource(location);
		readJsonConfig(json);
		linkData();
	}
	
	private void readJsonConfig(JsonObject json) {
		
		if(json != null) {
			for(Entry<String, JsonElement> entry : json.entrySet()) {
				
				String key = entry.getKey();
				JsonObject element = entry.getValue().getAsJsonObject();
				
				if("dimensions".equals(key)) {
					readDimensions(element);
				}
				else
				if("databases".equals(key)) {
					readDataBases(element);
				}
				else
				if("populators".equals(key)) {
					readPopulators(element);
				}
				
			}
		}
	}

	private void readDimensions(JsonObject element) {
		for(Entry<String, JsonElement> entry : element.entrySet()) {
			String key = entry.getKey();
			try {
				int dimensionId = Integer.parseInt(key);
				String dbaseName = entry.getValue().getAsString();
				dimensionMap.put(dimensionId, dbaseName);
			} catch (NumberFormatException e) { }
		}
	}

	private void readDataBases(JsonObject element) {
		for(Entry<String, JsonElement> entry : element.entrySet()) {
			String dBaseName = entry.getKey();
			JsonElement popElementList = entry.getValue();
			List<String> popNames = new ArrayList<>();
			for(JsonElement popElement : popElementList.getAsJsonArray()) {
				popNames.add(popElement.getAsString());
			}
			dBaseMap.put(dBaseName, new BiomeDataBaseMultiPopulator(popNames));
		}
	}
	
	private void readPopulators(JsonObject element) {
		for(Entry<String, JsonElement> entry : element.entrySet()) {
			String populatorName = entry.getKey();
			System.out.println("Processing Populator: " + populatorName );
			JsonElement jsonElement = entry.getValue();
			populatorMap.put(populatorName, new JSONBiomeDataBasePopulator(jsonElement));
		}
	}

	private void linkData() {
		
		Map<String, BiomeDataBase> dBases = new HashMap<>();
		
		//Create and populate databases
		for(Entry<String, BiomeDataBaseMultiPopulator> dBaseEntry : dBaseMap.entrySet()) {
			String dBaseName = dBaseEntry.getKey();
			BiomeDataBase dBase = DEFAULT.equals(dBaseName) ? TreeGenerator.getTreeGenerator().getDefaultBiomeDataBase() : new BiomeDataBase();
			dBaseEntry.getValue().populate(dBase);
			dBases.put(dBaseName, dBase);
		}
		
		//Link dimensions to databases
		for(Entry<Integer, String> dimensionLink : dimensionMap.entrySet()) {
			int dimensionId = dimensionLink.getKey();
			String dBaseName = dimensionLink.getValue();
			if(dBases.containsKey(dBaseName)) {
				BiomeDataBase dBase = dBases.get(dBaseName);
				TreeGenerator.getTreeGenerator().linkDimensionToDataBase(dimensionId, dBase);
			}
		}
		
	}
	
	private class BiomeDataBaseMultiPopulator implements IBiomeDataBasePopulator {
		
		private List<String> populatorNameList = new ArrayList<>();
		
		public BiomeDataBaseMultiPopulator(List<String> populatorNameList) {
			this.populatorNameList = populatorNameList;
		}
		
		@Override
		public void populate(BiomeDataBase biomeDataBase) {
			for(String populatorName : populatorNameList) {
				if(DEFAULT.equals(populatorName)) {
					WorldGenRegistry.populateAsDefaultDataBase(biomeDataBase);
				}
				else 
				if(populatorMap.containsKey(populatorName)) {
					populatorMap.get(populatorName).populate(biomeDataBase);
				}
			}
		}
		
	}
}
