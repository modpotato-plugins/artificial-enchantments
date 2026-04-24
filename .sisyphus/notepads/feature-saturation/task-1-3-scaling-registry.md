# ScalingAlgorithmRegistry Implementation Notes

**Task:** 1.3 - Scaling Algorithm Registry  
**Date:** 2026-04-19  
**Status:** Completed  

## Files Created

### Public API (api/scaling/)
1. **ScalingAlgorithmRegistry.java** - Interface for the registry
   - `register(String, ScalingAlgorithm)` - Register custom algorithms
   - `get(String, double...)` - Get algorithm instance with parameters
   - `hasAlgorithm(String)` - Check if algorithm exists
   - `getRegisteredNames()` - Get all registered names
   - `getMetadata(String)` - Get algorithm metadata
   - `unregister(String)` - Remove custom algorithm

2. **ScalingAlgorithm.java** - Interface for algorithm definitions
   - `create(double...)` - Create LevelScaling instance
   - `getDescription()` - Algorithm description
   - `getParameterCount()` - Number of parameters
   - `getParameterNames()` - Parameter names

3. **ScalingAlgorithmMetadata.java** - Immutable metadata class
   - Stores name, description, parameter info, built-in flag

### Implementation (internal/scaling/)
1. **ScalingAlgorithmRegistryImpl.java**
   - Uses `ConcurrentHashMap<String, Entry>` for thread-safe storage
   - Pre-registers 6 built-in algorithms on construction
   - Case-insensitive algorithm name handling
   - Validates algorithms produce finite values on `get()`
   - Built-in algorithms cannot be unregistered

2. **ScalingRegistryHolder.java**
   - Provides access to registry without global singleton
   - Uses volatile reference for thread safety
   - Falls back to default registry if not initialized

3. **ScalingRegistryInitializer.java**
   - Lazy initialization using initialization-on-demand holder idiom
   - Thread-safe singleton pattern for default registry

### Tests
- **ScalingAlgorithmRegistryImplTest.java** - 40+ tests covering:
  - Built-in algorithm functionality
  - Case-insensitive lookups
  - Custom registration/unregistration
  - Finite value validation
  - Thread safety
  - Metadata retrieval

## Integration Points

### EnchantmentDefinition.Builder
Added `scaling(String algorithmName, double... params)` default method:
```java
default Builder scaling(@NotNull String algorithmName, double... params) {
    throw new UnsupportedOperationException("Implementation not available in API module");
}
```

Implementation in `EnchantmentDefinitionBuilder` uses `ScalingRegistryHolder.getRegistry()` to lookup and instantiate algorithms.

### ArtificialEnchantmentsAPI
Added `getScalingRegistry()` method to interface for public access to registry.

## Built-in Algorithms

| Name | Parameters | Formula |
|------|-----------|---------|
| LINEAR | base, increment | base + (level - 1) * increment |
| EXPONENTIAL | base, multiplier | base * multiplier^(level - 1) |
| DIMINISHING | maxValue, scalingFactor | max * (level / (level + factor)) |
| CONSTANT | value | value (same at all levels) |
| STEPPED | level1, value1, level2, value2, ... | Interpolated step values |
| DECAYING | maxValue, decayFactor | max * (1 - decayFactor^level) |

## Design Decisions

### Thread Safety
- Used `ConcurrentHashMap` for algorithm storage
- Used `putIfAbsent` to prevent race conditions during registration
- Made `builtInNames` an immutable `Set.copyOf()` snapshot after construction

### Validation
- Parameter count validation in each algorithm
- Finite value validation wrapper on returned LevelScaling
- Null and empty name checks with descriptive error messages

### Case Insensitivity
- All algorithm names normalized to UPPERCASE in registry
- Input names converted to uppercase for lookup
- Preserves original case in metadata for display

### Instance Pattern
- Avoided global static singleton by using holder pattern
- Holder can be initialized with specific instance
- Falls back to lazily-created default instance
- Allows for test mocking and different implementations

## Usage Examples

### Using named scaling in builder:
```java
EnchantmentDefinition def = EnchantmentDefinition.builder()
    .key(key)
    .displayName(name)
    .minLevel(1)
    .maxLevel(5)
    .scaling("LINEAR", 1.0, 0.5)  // base=1.0, increment=0.5
    .applicable(Material.DIAMOND_SWORD)
    .build();
```

### Registering custom algorithm:
```java
api.getScalingRegistry().register("LOGARITHMIC", new ScalingAlgorithm() {
    public LevelScaling create(double... params) {
        return level -> params[0] * Math.log(level + params[1]);
    }
    public String getDescription() { return "Logarithmic scaling"; }
    public int getParameterCount() { return 2; }
    public String[] getParameterNames() { 
        return new String[]{"coefficient", "offset"}; 
    }
});
```

## Test Results
All 40+ registry tests pass successfully, including:
- Built-in algorithm functionality
- Case-insensitive lookups
- Thread safety verification
- Finite value validation
- Custom registration/unregistration

## Backward Compatibility
- All existing `scaling(LevelScaling)` and `scaling(Function)` methods preserved
- New `scaling(String, double...)` is a default method on the interface
- No breaking changes to existing code
