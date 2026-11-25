package com.pwp.coreclient.gui.animations;

public class Easing {

    private Easing() {}

    // t = progress 0..1
    public static float linear(float t) {
        return t;
    }

    // Квадратичные
    public static float easeInQuad(float t) {
        return t * t;
    }

    public static float easeOutQuad(float t) {
        return t * (2 - t);
    }

    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    // Кубические
    public static float easeInCubic(float t) {
        return t * t * t;
    }

    public static float easeOutCubic(float t) {
        return (--t) * t * t + 1;
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1;
    }

    // Экспоненциальные
    public static float easeInExpo(float t) {
        return t == 0 ? 0 : (float) Math.pow(2, 10 * (t - 1));
    }

    public static float easeOutExpo(float t) {
        return t == 1 ? 1 : (float) (1 - Math.pow(2, -10 * t));
    }

    // Эластичные (для пружинистых анимаций)
    public static float easeOutElastic(float t) {
        if (t == 0 || t == 1) return t;
        return (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.075) * (2 * Math.PI) / 0.3) + 1);
    }

    public static float easeInElastic(float t) {
        if (t == 0 || t == 1) return t;
        return (float) (-Math.pow(2, 10 * (t - 1)) * Math.sin((t - 1.075) * (2 * Math.PI) / 0.3));
    }

    // Bounce (отскок)
    public static float easeOutBounce(float t) {
        if (t < 1 / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2 / 2.75f) {
            return 7.5625f * (t -= 1.5f / 2.75f) * t + 0.75f;
        } else if (t < 2.5 / 2.75f) {
            return 7.5625f * (t -= 2.25f / 2.75f) * t + 0.9375f;
        } else {
            return 7.5625f * (t -= 2.625f / 2.75f) * t + 0.984375f;
        }
    }

    // Back (с перелётом)
    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return (float) (1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2));
    }

    // Пульсация (для иконок, кнопок)
    public static float pulse(float t) {
        return (float) (0.5 + 0.5 * Math.sin(t * Math.PI * 2));
    }

    // Качание (для уведомлений об ошибке)
    public static float shake(float t) {
        return (float) Math.sin(t * Math.PI * 8) * (1 - t);
    }
}
