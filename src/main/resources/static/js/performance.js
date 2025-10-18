/**
 * AutoTrack Performance Optimization Script
 * Handles lazy loading, performance monitoring, and resource optimization
 */

class AutoTrackPerformance {
    constructor() {
        this.metrics = {
            startTime: performance.now(),
            domContentLoaded: null,
            windowLoaded: null,
            firstPaint: null,
            firstContentfulPaint: null
        };
        
        this.init();
    }

    init() {
        this.observePerformanceMetrics();
        this.setupLazyLoading();
        this.optimizeImages();
        this.preloadCriticalResources();
    }

    observePerformanceMetrics() {
        // Measure DOM Content Loaded
        document.addEventListener('DOMContentLoaded', () => {
            this.metrics.domContentLoaded = performance.now() - this.metrics.startTime;
        });

        // Measure Window Load
        window.addEventListener('load', () => {
            this.metrics.windowLoaded = performance.now() - this.metrics.startTime;
            this.reportMetrics();
        });

        // Observe Paint Metrics
        if ('PerformanceObserver' in window) {
            const observer = new PerformanceObserver((list) => {
                const entries = list.getEntries();
                entries.forEach((entry) => {
                    if (entry.name === 'first-paint') {
                        this.metrics.firstPaint = entry.startTime;
                    }
                    if (entry.name === 'first-contentful-paint') {
                        this.metrics.firstContentfulPaint = entry.startTime;
                    }
                });
            });
            observer.observe({ entryTypes: ['paint'] });
        }
    }

    setupLazyLoading() {
        // Lazy load images
        if ('IntersectionObserver' in window) {
            const imageObserver = new IntersectionObserver((entries, observer) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const img = entry.target;
                        if (img.dataset.src) {
                            img.src = img.dataset.src;
                            img.classList.add('loaded');
                            observer.unobserve(img);
                        }
                    }
                });
            });

            document.querySelectorAll('img[data-src]').forEach(img => {
                imageObserver.observe(img);
            });
        }

        // Lazy load non-critical CSS
        this.lazyLoadCSS();
    }

    lazyLoadCSS() {
        const criticalCSSLoaded = () => {
            const nonCriticalCSS = [
                // Add any non-critical CSS files here
            ];

            nonCriticalCSS.forEach(href => {
                const link = document.createElement('link');
                link.rel = 'stylesheet';
                link.href = href;
                link.media = 'print';
                link.onload = () => { link.media = 'all'; };
                document.head.appendChild(link);
            });
        };

        if (document.readyState === 'complete') {
            criticalCSSLoaded();
        } else {
            window.addEventListener('load', criticalCSSLoaded);
        }
    }

    optimizeImages() {
        // Add WebP support detection and fallback
        const supportsWebP = () => {
            const canvas = document.createElement('canvas');
            canvas.width = 1;
            canvas.height = 1;
            return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0;
        };

        if (supportsWebP()) {
            document.documentElement.classList.add('webp-support');
        }
    }

    preloadCriticalResources() {
        const criticalResources = [
            // Add critical resources to preload
        ];

        criticalResources.forEach(resource => {
            const link = document.createElement('link');
            link.rel = 'preload';
            link.href = resource.href;
            link.as = resource.as;
            if (resource.crossorigin) {
                link.crossOrigin = resource.crossorigin;
            }
            document.head.appendChild(link);
        });
    }

    reportMetrics() {
        // Send performance metrics to analytics if needed
        if (window.console && console.log) {
            console.log('AutoTrack Performance Metrics:', this.metrics);
        }

        // You can send metrics to your analytics service here
        // Example: analytics.track('page_performance', this.metrics);
    }

    // Utility function to load scripts asynchronously
    static loadScript(src, callback) {
        const script = document.createElement('script');
        script.src = src;
        script.async = true;
        
        if (callback) {
            script.onload = callback;
            script.onerror = () => console.warn(`Failed to load script: ${src}`);
        }
        
        document.head.appendChild(script);
        return script;
    }

    // Utility function to defer non-critical JavaScript
    static deferScript(callback) {
        if (document.readyState === 'complete') {
            callback();
        } else {
            window.addEventListener('load', callback);
        }
    }
}

// Initialize performance optimization
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new AutoTrackPerformance();
    });
} else {
    new AutoTrackPerformance();
}

// Export for use in other scripts
window.AutoTrackPerformance = AutoTrackPerformance;