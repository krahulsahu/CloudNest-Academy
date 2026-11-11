import { useState, useEffect, useRef } from 'react';
import { Link } from '../router';
import { BANNER_SLIDES } from '../config';

export function BannerSlider() {
  const [current, setCurrent] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const slides = BANNER_SLIDES;
  const timeoutRef = useRef<number | null>(null);

  const nextSlide = () => {
    setCurrent(prev => (prev + 1) % slides.length);
  };

  const prevSlide = () => {
    setCurrent(prev => (prev - 1 + slides.length) % slides.length);
  };

  useEffect(() => {
    if (isPaused || slides.length <= 1) return;

    timeoutRef.current = window.setTimeout(() => {
      nextSlide();
    }, 5000);

    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, [current, isPaused, slides.length]);

  if (!slides || slides.length === 0) return null;

  return (
    <div 
      className="banner-slider"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      <div 
        className="banner-slides-track"
        style={{ transform: `translateX(-${current * 100}%)` }}
      >
        {slides.map((s, idx) => (
          <div 
            key={idx} 
            className="banner-slide"
            style={{ backgroundImage: `url(${s.image})` }}
          >
            <div className="banner-overlay">
              <div className="banner-content">
                <h1>{s.title}</h1>
                {s.subtitle && <p>{s.subtitle}</p>}
                {s.ctaText && (
                  <Link to={s.ctaLink || '/courses'} className="btn banner-btn">
                    {s.ctaText}
                  </Link>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {slides.length > 1 && (
        <>
          <button 
            type="button" 
            className="slider-arrow slider-arrow-left" 
            onClick={prevSlide}
            aria-label="Previous slide"
          >
            &#10094;
          </button>
          <button 
            type="button" 
            className="slider-arrow slider-arrow-right" 
            onClick={nextSlide}
            aria-label="Next slide"
          >
            &#10095;
          </button>

          <div className="slider-dots">
            {slides.map((_, idx) => (
              <button
                key={idx}
                type="button"
                className={`slider-dot ${idx === current ? 'active' : ''}`}
                onClick={() => setCurrent(idx)}
                aria-label={`Go to slide ${idx + 1}`}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

