import { useState, useEffect, useRef } from 'react';
import usePrefersReducedMotion from './usePrefersReducedMotion';

/**
 * Animate a numeric value from zero to target.
 * Preserves prefixes (like '$') and decimal precision.
 * Respects prefers-reduced-motion by rendering the final value immediately.
 * 
 * @param {string|number} rawValue - Value from application state, e.g. "0", "12", "$0.00"
 * @param {number} duration - Animation duration in ms (default: 800ms)
 * @returns {string} The animated display string
 */
export const useCountUp = (rawValue, duration = 800) => {
  const prefersReduced = usePrefersReducedMotion();

  // Parse numeric target and any prefix/suffix
  const parseValue = (val) => {
    if (typeof val === 'number') {
      return { prefix: '', num: val, decimals: 0, suffix: '' };
    }
    const str = String(val).trim();
    const match = str.match(/^([^0-9.-]*)([0-9,.]+)(.*)$/);
    if (!match) {
      return { prefix: '', num: 0, decimals: 0, suffix: str };
    }
    const prefix = match[1];
    const numStr = match[2].replace(/,/g, '');
    const suffix = match[3];
    const decimals = numStr.includes('.') ? numStr.split('.')[1].length : 0;
    const num = parseFloat(numStr) || 0;
    return { prefix, num, decimals, suffix };
  };

  const { prefix, num, decimals, suffix } = parseValue(rawValue);
  const [displayValue, setDisplayValue] = useState(() => {
    if (prefersReduced) return rawValue;
    return `${prefix}${(0).toFixed(decimals)}${suffix}`;
  });

  const hasAnimatedRef = useRef(false);
  const prevTargetRef = useRef(num);

  useEffect(() => {
    if (prefersReduced || num === 0) {
      return;
    }

    // If already animated for this target value, avoid unnecessary re-animation
    if (hasAnimatedRef.current && prevTargetRef.current === num) {
      return;
    }

    prevTargetRef.current = num;
    hasAnimatedRef.current = true;

    let startTimestamp = null;
    let animationFrameId = null;

    const easeOutQuad = (t) => t * (2 - t);

    const step = (timestamp) => {
      if (!startTimestamp) startTimestamp = timestamp;
      const progress = Math.min((timestamp - startTimestamp) / duration, 1);
      const easedProgress = easeOutQuad(progress);
      const current = easedProgress * num;

      setDisplayValue(`${prefix}${current.toFixed(decimals)}${suffix}`);

      if (progress < 1) {
        animationFrameId = requestAnimationFrame(step);
      } else {
        setDisplayValue(`${prefix}${num.toFixed(decimals)}${suffix}`);
      }
    };

    animationFrameId = requestAnimationFrame(step);

    return () => {
      if (animationFrameId) {
        cancelAnimationFrame(animationFrameId);
      }
    };
  }, [num, prefix, decimals, suffix, duration, prefersReduced]);

  return prefersReduced ? rawValue : displayValue;
};

export default useCountUp;
