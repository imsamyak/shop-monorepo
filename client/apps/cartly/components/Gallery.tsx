'use client';

import { useState } from 'react';
import { PLACEHOLDER_IMAGE } from '@/lib/images';

/** The first picture is the main one; the thumbnails below show the rest in the order they were given. */
export function Gallery({ images, title }: { images: string[]; title: string }) {
  const [current, setCurrent] = useState(0);
  const pictures = images.length ? images : [PLACEHOLDER_IMAGE];

  return (
    <div className="gallery">
      <div className="gallery-main">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={pictures[current] ?? PLACEHOLDER_IMAGE} alt={title} />
      </div>
      {pictures.length > 1 && (
        <ul className="thumbs">
          {pictures.map((url, position) => (
            <li key={url}>
              <button
                type="button"
                className={position === current ? 'on' : undefined}
                aria-label={`Picture ${position + 1}`}
                aria-current={position === current}
                onClick={() => setCurrent(position)}
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={url} alt="" loading="lazy" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
