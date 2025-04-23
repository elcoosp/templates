export function Button({
  title,
  onPress,
  disabled = false,
  variant = "primary",
  size = "medium",
  className = "",
  ...props
}: {
  title: string;
  onPress: () => any;
  disabled?: boolean;
  variant?: "primary" | "secondary" | "danger" | "success";
  size?: "small" | "medium" | "large";
  className?: string;
  [x: string]: any;
}) {
  // Determine button styles based on variant
  const variantStyles = {
    primary: "bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white",
    secondary: "bg-gray-200 hover:bg-gray-300 active:bg-gray-400 text-gray-800",
    danger: "bg-red-600 hover:bg-red-700 active:bg-red-800 text-white",
    success: "bg-green-600 hover:bg-green-700 active:bg-green-800 text-white",
  }[variant];

  // Determine button size
  const sizeStyles = {
    small: "px-3 py-1 text-sm",
    medium: "px-4 py-2",
    large: "px-6 py-3 text-lg",
  }[size];

  // Handle disabled state
  const disabledStyles = disabled
    ? "opacity-50 cursor-not-allowed"
    : "cursor-pointer";

  return (
    <view
      bindtap={disabled ? undefined : onPress}
      className={`
        w-full rounded-md font-medium transition-colors duration-200
        flex justify-center items-center shadow-sm
        focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500
        ${variantStyles}
        ${sizeStyles}
        ${disabledStyles}
        ${className}
      `}
      {...props}
    >
      <text className="text-center">{title}</text>
    </view>
  );
}
