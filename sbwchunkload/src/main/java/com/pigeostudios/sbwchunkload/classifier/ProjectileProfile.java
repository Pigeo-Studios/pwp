package com.pigeostudios.sbwchunkload.classifier;

import com.pigeostudios.sbwchunkload.config.ChunkLoadingConfig;

/**
 * Профиль поведения снаряда. Вся логика «как часто обновлять и как далеко
 * смотреть вперёд» лежит здесь — Tracker/Corridor/TicketManager ничего об
 * этом не знают.
 *
 * Значения интервалов и упреждений читаются из конфига в рантайме
 * (ForgeConfigSpec перезагружается без перекомпиляции — серверная команда
 * /reload или правка toml + перезапуск).
 */
public enum ProjectileProfile {

    /** Быстрые линейные: SBW-пули (ProjectileEntity), тазеры — ~24 блока/тик. */
    FAST_LINEAR {
        @Override
        public int refreshInterval() {
            return ChunkLoadingConfig.BULLET_INTERVAL.get();
        }

        @Override
        public int minimumLookahead() {
            return ChunkLoadingConfig.BULLET_MIN_LOOKAHEAD.get();
        }

        @Override
        public int maximumLookahead() {
            return ChunkLoadingConfig.BULLET_MAX_LOOKAHEAD.get();
        }
    },

    /** Медленные линейные: pointblank/FCL-ракеты, ПТУРы, НУРС. */
    SLOW_LINEAR {
        @Override
        public int refreshInterval() {
            return ChunkLoadingConfig.ROCKET_INTERVAL.get();
        }

        @Override
        public int minimumLookahead() {
            return ChunkLoadingConfig.ROCKET_MIN_LOOKAHEAD.get();
        }

        @Override
        public int maximumLookahead() {
            return ChunkLoadingConfig.ROCKET_MAX_LOOKAHEAD.get();
        }
    },

    /** Тяжёлые снаряды автопушек/пушек: быстрые, но крупные. */
    HEAVY {
        @Override
        public int refreshInterval() {
            return ChunkLoadingConfig.HEAVY_INTERVAL.get();
        }

        @Override
        public int minimumLookahead() {
            return ChunkLoadingConfig.HEAVY_MIN_LOOKAHEAD.get();
        }

        @Override
        public int maximumLookahead() {
            return ChunkLoadingConfig.HEAVY_MAX_LOOKAHEAD.get();
        }
    },

    /** Бомбы/падающее вооружение: медленные, часто с гравитацией. */
    BALLISTIC {
        @Override
        public int refreshInterval() {
            return ChunkLoadingConfig.BALLISTIC_INTERVAL.get();
        }

        @Override
        public int minimumLookahead() {
            return ChunkLoadingConfig.BALLISTIC_MIN_LOOKAHEAD.get();
        }

        @Override
        public int maximumLookahead() {
            return ChunkLoadingConfig.BALLISTIC_MAX_LOOKAHEAD.get();
        }
    },

    /** Не летит (клейморы, мины и пр.): не трекается, только классифицируется. */
    STATIC {
        @Override
        public int refreshInterval() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int minimumLookahead() {
            return 0;
        }

        @Override
        public int maximumLookahead() {
            return 0;
        }
    };

    /** Через сколько тиков обновлять снаряд этого профиля. */
    public abstract int refreshInterval();

    /** Минимальное упреждение в блоках. */
    public abstract int minimumLookahead();

    /** Максимальное упреждение в блоках. */
    public abstract int maximumLookahead();
}
