package io.artificial.enchantments.internal.scaling;

import io.artificial.enchantments.api.scaling.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe implementation of ScalingAlgorithmRegistry.
 *
 * <p>Uses ConcurrentHashMap for thread-safe concurrent access.
 * Pre-registers all built-in algorithms on construction.
 *
 * @since 0.2.0
 */
public final class ScalingAlgorithmRegistryImpl implements ScalingAlgorithmRegistry {

    private final Map<String, Entry> algorithms = new ConcurrentHashMap<>();
    private final Set<String> builtInNames;

    /**
     * Creates a new registry and pre-registers all built-in algorithms.
     */
    public ScalingAlgorithmRegistryImpl() {
        registerLinear();
        registerExponential();
        registerDiminishing();
        registerConstant();
        registerStepped();
        registerDecaying();

        this.builtInNames = Set.copyOf(algorithms.keySet());
    }

    private void registerLinear() {
        String name = "LINEAR";
        ScalingAlgorithm algorithm = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                requireParamCount(params, 2, "LINEAR requires 2 parameters: base, increment");
                return LevelScaling.linear(params[0], params[1]);
            }

            @Override
            public String getDescription() {
                return "Linear scaling: base + (level - 1) * increment";
            }

            @Override
            public int getParameterCount() {
                return 2;
            }

            @Override
            public String[] getParameterNames() {
                return new String[]{"base", "increment"};
            }
        };
        algorithms.put(name, new Entry(name, algorithm, true));
    }

    private void registerExponential() {
        String name = "EXPONENTIAL";
        ScalingAlgorithm algorithm = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                requireParamCount(params, 2, "EXPONENTIAL requires 2 parameters: base, multiplier");
                return LevelScaling.exponential(params[0], params[1]);
            }

            @Override
            public String getDescription() {
                return "Exponential scaling: base * multiplier^(level - 1)";
            }

            @Override
            public int getParameterCount() {
                return 2;
            }

            @Override
            public String[] getParameterNames() {
                return new String[]{"base", "multiplier"};
            }
        };
        algorithms.put(name, new Entry(name, algorithm, true));
    }

    private void registerDiminishing() {
        String name = "DIMINISHING";
        ScalingAlgorithm algorithm = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                requireParamCount(params, 2, "DIMINISHING requires 2 parameters: maxValue, scalingFactor");
                return LevelScaling.diminishing(params[0], params[1]);
            }

            @Override
            public String getDescription() {
                return "Diminishing returns: maxValue * (level / (level + scalingFactor))";
            }

            @Override
            public int getParameterCount() {
                return 2;
            }

            @Override
            public String[] getParameterNames() {
                return new String[]{"maxValue", "scalingFactor"};
            }
        };
        algorithms.put(name, new Entry(name, algorithm, true));
    }

    private void registerConstant() {
        String name = "CONSTANT";
        ScalingAlgorithm algorithm = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                requireParamCount(params, 1, "CONSTANT requires 1 parameter: value");
                return LevelScaling.constant(params[0]);
            }

            @Override
            public String getDescription() {
                return "Constant scaling: same value at all levels";
            }

            @Override
            public int getParameterCount() {
                return 1;
            }

            @Override
            public String[] getParameterNames() {
                return new String[]{"value"};
            }
        };
        algorithms.put(name, new Entry(name, algorithm, true));
    }

    private void registerStepped() {
        String name = "STEPPED";
        ScalingAlgorithm algorithm = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                requireEvenParamCount(params, "STEPPED requires even number of parameters: level1, value1, level2, value2, ...");
                Map<Integer, Double> steps = new TreeMap<>();
                for (int i = 0; i < params.length; i += 2) {
                    int level = (int) params[i];
                    if (level < 1) {
                        throw new IllegalArgumentException("Step levels must be >= 1, got: " + level);
                    }
                    steps.put(level, params[i + 1]);
                }
                return LevelScaling.stepped(steps);
            }

            @Override
            public String getDescription() {
                return "Stepped scaling: custom values at specific levels with interpolation";
            }

            @Override
            public int getParameterCount() {
                return -1; // Variable number
            }

            @Override
            public String[] getParameterNames() {
                return new String[]{"level1", "value1", "level2", "value2", "..."};
            }
        };
        algorithms.put(name, new Entry(name, algorithm, true));
    }

    private void registerDecaying() {
        String name = "DECAYING";
        ScalingAlgorithm algorithm = new ScalingAlgorithm() {
            @Override
            public LevelScaling create(double... params) {
                requireParamCount(params, 2, "DECAYING requires 2 parameters: maxValue, decayFactor");
                return new DecayingScaling(params[0], params[1]);
            }

            @Override
            public String getDescription() {
                return "Exponential decay: maxValue * (1 - decayFactor^level)";
            }

            @Override
            public int getParameterCount() {
                return 2;
            }

            @Override
            public String[] getParameterNames() {
                return new String[]{"maxValue", "decayFactor"};
            }
        };
        algorithms.put(name, new Entry(name, algorithm, true));
    }

    private void requireParamCount(double[] params, int expected, String message) {
        if (params.length != expected) {
            throw new IllegalArgumentException(message + ", got: " + params.length);
        }
    }

    private void requireEvenParamCount(double[] params, String message) {
        if (params.length < 2 || params.length % 2 != 0) {
            throw new IllegalArgumentException(message + ", got: " + params.length);
        }
    }

    @Override
    public void register(@NotNull String name, @NotNull ScalingAlgorithm algorithm) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty");
        }

        String upperName = name.toUpperCase(Locale.ROOT);
        Entry entry = new Entry(upperName, algorithm, false);

        Entry previous = algorithms.putIfAbsent(upperName, entry);
        if (previous != null) {
            throw new IllegalStateException("Algorithm already registered: " + upperName);
        }
    }

    @Override
    @NotNull
    public LevelScaling get(@NotNull String name, double... params) {
        Objects.requireNonNull(name, "name must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty");
        }

        String upperName = name.toUpperCase(Locale.ROOT);
        Entry entry = algorithms.get(upperName);

        if (entry == null) {
            throw new IllegalArgumentException("Algorithm not found: " + name);
        }

        LevelScaling scaling = entry.algorithm.create(params);
        return validateFinite(scaling, name);
    }

    private LevelScaling validateFinite(LevelScaling scaling, String name) {
        return level -> {
            double value = scaling.calculate(level);
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(
                    "Algorithm " + name + " produced non-finite value at level " + level + ": " + value
                );
            }
            return value;
        };
    }

    @Override
    public boolean hasAlgorithm(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        return algorithms.containsKey(name.toUpperCase(Locale.ROOT));
    }

    @Override
    @NotNull
    public Set<String> getRegisteredNames() {
        return Collections.unmodifiableSet(algorithms.keySet());
    }

    @Override
    @NotNull
    public Optional<ScalingAlgorithmMetadata> getMetadata(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        Entry entry = algorithms.get(name.toUpperCase(Locale.ROOT));

        if (entry == null) {
            return Optional.empty();
        }

        ScalingAlgorithmMetadata metadata = new ScalingAlgorithmMetadata(
            entry.name,
            entry.algorithm.getDescription(),
            entry.algorithm.getParameterCount(),
            entry.algorithm.getParameterNames(),
            entry.builtIn
        );

        return Optional.of(metadata);
    }

    @Override
    public boolean unregister(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        String upperName = name.toUpperCase(Locale.ROOT);

        if (builtInNames.contains(upperName)) {
            return false; // Cannot unregister built-in algorithms
        }

        return algorithms.remove(upperName) != null;
    }

    /**
     * Internal entry holding algorithm and metadata.
     */
    private static final class Entry {
        final String name;
        final ScalingAlgorithm algorithm;
        final boolean builtIn;

        Entry(String name, ScalingAlgorithm algorithm, boolean builtIn) {
            this.name = name;
            this.algorithm = algorithm;
            this.builtIn = builtIn;
        }
    }
}
