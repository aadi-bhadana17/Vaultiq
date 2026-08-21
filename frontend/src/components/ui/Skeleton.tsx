interface SkeletonProps {
  height?: number;
}

export function Skeleton({ height = 18 }: SkeletonProps) {
  return <div className="skeleton" style={{ height }} aria-hidden="true" />;
}
