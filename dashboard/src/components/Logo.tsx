import React from 'react';

interface LogoProps {
  className?: string;
  showText?: boolean;
}

const Logo: React.FC<LogoProps> = ({ className = "w-8 h-8", showText = true }) => {
  return (
    <div className="flex items-center gap-2">
      {/* Professional logo SVG - A modern display/screen icon representing OOH advertising */}
      <svg
        className={className}
        viewBox="0 0 32 32"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        {/* Screen/Display base */}
        <rect
          x="4"
          y="6"
          width="24"
          height="18"
          rx="2"
          fill="currentColor"
        />
        {/* Screen content - bars representing content/advertisements */}
        <rect
          x="7"
          y="9"
          width="18"
          height="3"
          rx="0.5"
          fill="currentColor"
          opacity="0.7"
        />
        <rect
          x="7"
          y="14"
          width="12"
          height="3"
          rx="0.5"
          fill="currentColor"
          opacity="0.5"
        />
        <rect
          x="7"
          y="19"
          width="15"
          height="3"
          rx="0.5"
          fill="currentColor"
          opacity="0.6"
        />
        {/* Stand/base */}
        <rect
          x="14"
          y="24"
          width="4"
          height="2"
          rx="1"
          fill="currentColor"
        />
      </svg>
      {showText && (
        <span className="text-lg font-semibold text-white">Mnemocast</span>
      )}
    </div>
  );
};

export default Logo;

