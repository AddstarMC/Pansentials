package au.com.addstar.pansentials;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MasterPlugin extends JavaPlugin
{
	private HashMap<String, Module> mLoadedModules;
	
	private HashMap<String, ModuleDefinition> mAvailableModules;
	private HashMap<String, ModuleDefinition> mAvailableModulesByName;
	
	private Config mConfig;
	private FileConfiguration format;
	
	private static MasterPlugin mInstance;
	public static MasterPlugin getInstance()
	{
		return mInstance;
	}
	
	public MasterPlugin()
	{
		mAvailableModules = new HashMap<String, ModuleDefinition>();
		mAvailableModulesByName = new HashMap<String, ModuleDefinition>();
		
		mLoadedModules = new HashMap<String, Module>();
	}
	
	private void registerModules()
	{
		registerModule("FlyModule", "au.com.addstar.pansentials.modules.FlyModule");
		registerModule("HatModule", "au.com.addstar.pansentials.modules.HatModule");
		registerModule("WorkbenchModule", "au.com.addstar.pansentials.modules.WorkbenchModule");
		registerModule("HealModule", "au.com.addstar.pansentials.modules.HealModule");
		registerModule("SpeedModule", "au.com.addstar.pansentials.modules.SpeedModule");
		registerModule("ExpModule", "au.com.addstar.pansentials.modules.ExpModule");
		registerModule("WhoIsModule", "au.com.addstar.pansentials.modules.WhoIsModule");
		registerModule("DropItemModule", "au.com.addstar.pansentials.modules.DropItemModule");
		registerModule("EnchantModule", "au.com.addstar.pansentials.modules.EnchantModule");
		registerModule("GamemodeModule", "au.com.addstar.pansentials.modules.GamemodeModule");
		registerModule("ModeModule", "au.com.addstar.pansentials.modules.MoreModule");
		registerModule("RepairModule", "au.com.addstar.pansentials.modules.RepairModule");
		registerModule("GodModule", "au.com.addstar.pansentials.modules.GodModule");
		registerModule("TimeModule", "au.com.addstar.pansentials.modules.TimeModule");
		registerModule("SmiteModule", "au.com.addstar.pansentials.modules.SmiteModule");
		registerModule("StackModule", "au.com.addstar.pansentials.modules.StackModule");
		registerModule("PowerTool", "au.com.addstar.pansentials.modules.PowertoolModule");
		registerModule("SpawnMob", "au.com.addstar.pansentials.modules.SpawnMobModule");
		//TODO: Register additional modules here
	}
	
	@Override
	public void onEnable()
	{
		mInstance = this;
		mConfig = new Config(new File(getDataFolder(), "config.yml"));
		
		getDataFolder().mkdir();
		
		if(mConfig.load())
			mConfig.save();
		
		reloadFormat();
		
		PandoraCommand cmd = new PandoraCommand(this);
		getCommand("pansentials").setExecutor(cmd);
		getCommand("pansentials").setTabCompleter(cmd);
		
		registerModules();
		loadModules();
	}
	
	@Override
	public void onDisable()
	{
		for(Entry<String, Module> module : mLoadedModules.entrySet())
		{
			try
			{
				module.getValue().onDisable();
			}
			catch(Throwable e)
			{
				getLogger().severe("Error disabling module: " + module.getKey());
				e.printStackTrace();
			}
		}
		
		mLoadedModules.clear();
		mAvailableModules.clear();
		mAvailableModulesByName.clear();
		mInstance = null;
	}

	public final boolean isModuleLoaded(String module)
	{
		return mLoadedModules.containsKey(module);
	}
	
	public final Set<String> getAllModules()
	{
		return Collections.unmodifiableSet(mAvailableModulesByName.keySet());
	}
	
	public final boolean reloadModule(String module)
	{
		if(!isModuleLoaded(module))
			return loadModule(module);
		
		Module instance = mLoadedModules.get(module);
		
		mLoadedModules.remove(module);
		
		try
		{
			instance.onDisable();
		}
		catch(Throwable e)
		{
			getLogger().severe("Error disabling module: " + module);
			e.printStackTrace();
			return false;
		}
		
		try
		{
			instance.onEnable();
		}
		catch(Throwable e)
		{
			getLogger().severe("Error enabling module: " + module);
			e.printStackTrace();
			return false;
		}
		
		mLoadedModules.put(module, instance);
		return true;
	}
	
	public final boolean enableModule(String module)
	{
		if(isModuleLoaded(module))
			return false;
		
		return loadModule(module);
	}
	
	public final boolean disableModule(String module)
	{
		if(!isModuleLoaded(module))
			return false;
		
		Module instance = mLoadedModules.get(module);
		
		mLoadedModules.remove(module);
		
		try
		{
			instance.onDisable();
			if(instance instanceof Listener)
				HandlerList.unregisterAll((Listener)instance);
		}
		catch(Throwable e)
		{
			getLogger().severe("Error disabling module: " + module);
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Registers a module for loading
	 * @param name Name of module
	 * @param moduleClass Class for the module
	 * @param dependencies Names of plugins needed for this module to load
	 */
	public void registerModule(String name, String moduleClass, String... dependencies)
	{
		ModuleDefinition def = new ModuleDefinition(name, moduleClass, dependencies);
		mAvailableModules.put(moduleClass, def);
		mAvailableModulesByName.put(name, def);
	}
	
	/**
	 * Registers a module with NMS code for loading
	 * @param name Name of module
	 * @param moduleClass Class for the module
	 * @param version The CB version that must be loaded. This should be in the form of "1_7_R4"
	 * @param dependencies Names of plugins needed for this module to load
	 */
	public void registerNMSModule(String name, String moduleClass, String version, String... dependencies)
	{
		if(!version.equals(getCBVersion()))
		{
			getLogger().severe("[NMS Module] Cannot load " + name + ". Required: " + version + " has: " + getCBVersion());
			return;
		}
		
		registerModule(name, moduleClass, dependencies);
	}
	
	private String mCBVersion = null;
	private String getCBVersion()
	{
		if(mCBVersion == null)
		{
			String name = Bukkit.getServer().getClass().getName();
			name = name.substring("org.bukkit.craftbukkit.v".length());
			name = name.substring(0,name.indexOf("."));
			mCBVersion = name;
		}
		
		return mCBVersion;
	}
	
	private void loadModules()
	{
		mLoadedModules.clear();
		
		for(String name : mAvailableModulesByName.keySet())
		{
			if(!mConfig.disabledModules.contains(name.toLowerCase()))
				loadModule(name);
			else
				getLogger().info(String.format("[%s] Not enabling, disabled from config", name));
		}
	}
	
	private boolean loadModule(String name)
	{
		ModuleDefinition module = mAvailableModulesByName.get(name);
		
		String missingDeps = "";
		
		for(String plugin : module.dependencies)
		{
			if(!Bukkit.getPluginManager().isPluginEnabled(plugin))
			{
				if(!missingDeps.isEmpty())
					missingDeps += ", ";
				missingDeps += plugin;
			}
		}
		
		if(!missingDeps.isEmpty())
		{
			getLogger().info(String.format("[%s] Not enabling, missing dependencies: %s", name, missingDeps));
			return false;
		}
		
		Module instance = createModule(module.name, module.moduleClass);
		
		if(instance == null)
			return false;
		
		mLoadedModules.put(module.name, instance);
		
		return true;
	}
	
	private Module createModule(String name, String moduleClass)
	{
		try
		{
			Class<?> rawClazz = Class.forName(moduleClass);
			if(!Module.class.isAssignableFrom(rawClazz))
			{
				getLogger().severe("Module class '" + moduleClass + "' is not an instance of Module!");
				return null;
			}
			
			Module module = rawClazz.asSubclass(Module.class).newInstance();
			module.setPandoraInstance(this);
			
			try
			{
				module.onEnable();
				if(module instanceof Listener)
					Bukkit.getPluginManager().registerEvents((Listener)module, this);
				
				return module;
			}
			catch(Throwable e)
			{
				getLogger().severe("Failed to enable module: " + name);
				e.printStackTrace();
			}
		}
		catch(InstantiationException e)
		{
			getLogger().severe("Failed to instanciate " + name);
			e.printStackTrace();
		}
		catch(ExceptionInInitializerError e)
		{
			getLogger().severe("Failed to instanciate " + name);
			e.printStackTrace();
		}
		catch ( IllegalAccessException e )
		{
			getLogger().severe("Failed to instanciate " + name + ". No public default constructor available.");
			e.printStackTrace();
		}
		catch ( ClassNotFoundException e )
		{
			getLogger().severe("Failed to instanciate " + name + ". Class not found");
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static class Config extends AutoConfig
	{
		public Config(File file)
		{
			super(file);
		}
		
		@ConfigField()
		public HashSet<String> disabledModules = new HashSet<String>();
		
		@Override
		protected void onPostLoad() throws InvalidConfigurationException
		{
			HashSet<String> lowerCaseSet = new HashSet<String>(disabledModules.size());
			
			for(String name : disabledModules)
				lowerCaseSet.add(name.toLowerCase());
			
			disabledModules = lowerCaseSet;
		}
	}
	
	private static class ModuleDefinition
	{
		public final String name;
		public final String moduleClass;
		public final String[] dependencies;
		public ModuleDefinition(String name, String moduleClass, String... dependencies)
		{
			this.name = name;
			this.moduleClass = moduleClass;
			if(dependencies == null)
				this.dependencies = new String[0];
			else
				this.dependencies = dependencies;
		}
	}
	
	@SuppressWarnings("deprecation")
	public void reloadFormat(){
		File f = new File(getDataFolder() + "/format.yml");
		if(!f.exists()){
			saveResource("format.yml", false);
		}
		f = new File(getDataFolder() + "/format.yml");
		format = YamlConfiguration.loadConfiguration(f);
		FileConfiguration inConf = YamlConfiguration.loadConfiguration(getResource("format.yml"));
		for(String key : inConf.getKeys(true)){
			if(!format.contains(key)){
				format.set(key, inConf.get(key));
			}
		}
		
		try {
			format.save(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public FileConfiguration getFormatConfig(){
		return format;
	}
}
