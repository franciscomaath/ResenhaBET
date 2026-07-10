/* @ds-bundle: {"format":4,"namespace":"ResenhaBETDesignSystem_b0ab9a","components":[{"name":"Button","sourcePath":"components/core/Button.jsx"},{"name":"Card","sourcePath":"components/core/Card.jsx"},{"name":"Fab","sourcePath":"components/core/Fab.jsx"},{"name":"Icon","sourcePath":"components/core/Icon.jsx"},{"name":"Input","sourcePath":"components/core/Input.jsx"},{"name":"MoneyValue","sourcePath":"components/data-display/MoneyValue.jsx"},{"name":"OddButton","sourcePath":"components/data-display/OddButton.jsx"},{"name":"RankRow","sourcePath":"components/data-display/RankRow.jsx"},{"name":"RoleChip","sourcePath":"components/data-display/RoleChip.jsx"},{"name":"SectionHeader","sourcePath":"components/data-display/SectionHeader.jsx"},{"name":"StatCell","sourcePath":"components/data-display/StatCell.jsx"},{"name":"StatusBadge","sourcePath":"components/data-display/StatusBadge.jsx"},{"name":"EmptyState","sourcePath":"components/feedback/EmptyState.jsx"},{"name":"LoadingSkeleton","sourcePath":"components/feedback/LoadingSkeleton.jsx"},{"name":"StateBanner","sourcePath":"components/feedback/StateBanner.jsx"}],"sourceHashes":{"components/core/Button.jsx":"aabdf368100d","components/core/Card.jsx":"2f3e1fd35342","components/core/Fab.jsx":"8e96d46ea45b","components/core/Icon.jsx":"8bd539d91a18","components/core/Input.jsx":"9ebfba4ef713","components/data-display/MoneyValue.jsx":"84e4275f026f","components/data-display/OddButton.jsx":"f98ef5199920","components/data-display/RankRow.jsx":"155f9eafe2cb","components/data-display/RoleChip.jsx":"f76648031afb","components/data-display/SectionHeader.jsx":"1c6e22229909","components/data-display/StatCell.jsx":"fd3f2f5144c1","components/data-display/StatusBadge.jsx":"fd3cb6a98118","components/feedback/EmptyState.jsx":"3e696fdda65e","components/feedback/LoadingSkeleton.jsx":"89591eaf4f8b","components/feedback/StateBanner.jsx":"fb5e5bab230c","ui_kits/mobile-app/auth.jsx":"73966052a36d","ui_kits/mobile-app/data.js":"910c5d173f78","ui_kits/mobile-app/screens.jsx":"f68fb7685961","ui_kits/mobile-app/shell.jsx":"894af58cbcbe"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.ResenhaBETDesignSystem_b0ab9a = window.ResenhaBETDesignSystem_b0ab9a || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/core/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * ResenhaBET primary button.
 * variant: "primary" (green — actions/money), "secondary" (subdued gray),
 * "accent" (blue — navigational), "destructive" (red), "ghost".
 */
function Button({
  variant = 'primary',
  size = 'md',
  block = false,
  disabled = false,
  loading = false,
  iconLeft,
  iconRight,
  children,
  style,
  ...rest
}) {
  const sizes = {
    sm: {
      minHeight: 36,
      padding: '0 14px',
      fontSize: 13
    },
    md: {
      minHeight: 44,
      padding: '0 20px',
      fontSize: 14
    },
    lg: {
      minHeight: 52,
      padding: '0 24px',
      fontSize: 15
    }
  };
  const variants = {
    primary: {
      background: 'var(--primary)',
      color: '#fff'
    },
    secondary: {
      background: 'var(--color-gray-700)',
      color: 'var(--text-secondary)'
    },
    accent: {
      background: 'var(--accent)',
      color: '#fff'
    },
    destructive: {
      background: 'var(--danger-hover)',
      color: '#fff'
    },
    ghost: {
      background: 'transparent',
      color: 'var(--accent)'
    }
  };
  return /*#__PURE__*/React.createElement("button", _extends({
    disabled: disabled || loading,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8,
      width: block ? '100%' : undefined,
      border: 'none',
      borderRadius: 'var(--radius-md)',
      fontFamily: 'var(--font-sans)',
      fontWeight: 700,
      cursor: disabled || loading ? 'not-allowed' : 'pointer',
      opacity: disabled || loading ? 0.5 : 1,
      transition: 'background var(--dur-fast) var(--ease-standard), transform var(--dur-fast)',
      ...sizes[size],
      ...variants[variant],
      ...style
    }
  }, rest), loading && /*#__PURE__*/React.createElement("span", {
    style: {
      width: 16,
      height: 16,
      borderRadius: '50%',
      border: '2px solid rgba(255,255,255,0.4)',
      borderTopColor: '#fff',
      animation: 'rb-spin 0.7s linear infinite'
    }
  }), !loading && iconLeft && /*#__PURE__*/React.createElement("span", {
    className: "material-icons",
    style: {
      fontSize: '1.2em'
    }
  }, iconLeft), children, !loading && iconRight && /*#__PURE__*/React.createElement("span", {
    className: "material-icons",
    style: {
      fontSize: '1.2em'
    }
  }, iconRight), /*#__PURE__*/React.createElement("style", null, `@keyframes rb-spin{to{transform:rotate(360deg)}}`));
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Button.jsx", error: String((e && e.message) || e) }); }

// components/core/Card.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Base surface card. rounded-2xl (16px), hairline border, dark surface.
 * elevated=true uses a stronger background + shadow.
 */
function Card({
  elevated = false,
  padding,
  children,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      borderRadius: 'var(--radius-lg)',
      border: `1px solid ${elevated ? 'var(--border-strong)' : 'var(--border-hairline)'}`,
      background: elevated ? 'var(--color-gray-800)' : 'var(--surface-2)',
      boxShadow: elevated ? 'var(--shadow-card)' : 'none',
      padding: padding ?? 'var(--card-pad-lg)',
      color: 'var(--text-primary)',
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { Card });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Card.jsx", error: String((e && e.message) || e) }); }

// components/core/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Material Icons glyph. ResenhaBET's sole icon system. */
function Icon({
  name,
  size = 20,
  color = 'currentColor',
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("span", _extends({
    className: "material-icons",
    style: {
      fontSize: size,
      color,
      lineHeight: 1,
      ...style
    }
  }, rest), name);
}
Object.assign(__ds_scope, { Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Icon.jsx", error: String((e && e.message) || e) }); }

// components/core/Fab.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Floating Action Button — shell-owned primary creation action. 56×56, blue. */
function Fab({
  icon = 'add',
  label,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("button", _extends({
    "aria-label": label,
    title: label,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: 'var(--fab-size)',
      height: 'var(--fab-size)',
      borderRadius: '50%',
      border: 'none',
      background: 'var(--accent)',
      color: '#fff',
      cursor: 'pointer',
      boxShadow: '0 8px 24px rgba(61,139,253,0.35)',
      transition: 'background var(--dur-fast) var(--ease-standard)',
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 26
  }));
}
Object.assign(__ds_scope, { Fab });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Fab.jsx", error: String((e && e.message) || e) }); }

// components/core/Input.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Standard dark form input. Full-width, 44px min height. */
function Input({
  invalid = false,
  prefix,
  style,
  ...rest
}) {
  const field = /*#__PURE__*/React.createElement("input", _extends({
    style: {
      width: '100%',
      minHeight: 'var(--touch-min)',
      boxSizing: 'border-box',
      borderRadius: 'var(--radius-sm)',
      border: `1px solid ${invalid ? 'var(--danger)' : 'var(--border-strong)'}`,
      background: 'var(--surface-3)',
      color: 'var(--text-primary)',
      fontFamily: 'var(--font-sans)',
      fontSize: 14,
      padding: prefix ? '12px 12px 12px 32px' : '12px',
      outline: 'none',
      ...style
    }
  }, rest));
  if (!prefix) return field;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'relative',
      display: 'block'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      position: 'absolute',
      left: 12,
      top: '50%',
      transform: 'translateY(-50%)',
      color: 'var(--text-muted)',
      fontSize: 14,
      pointerEvents: 'none'
    }
  }, prefix), field);
}
Object.assign(__ds_scope, { Input });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Input.jsx", error: String((e && e.message) || e) }); }

// components/data-display/MoneyValue.jsx
try { (() => {
/** Formatted BRL money. Green by default; use tone for positive/neutral/loss. */
function MoneyValue({
  amount,
  tone = 'money',
  size = 16,
  prefix = 'R$',
  style
}) {
  const colors = {
    money: 'var(--money)',
    positive: 'var(--money-positive)',
    neutral: 'var(--text-primary)',
    loss: 'var(--danger)'
  };
  const formatted = Number(amount).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  return /*#__PURE__*/React.createElement("span", {
    className: "tnum",
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: size,
      color: colors[tone] ?? colors.money,
      ...style
    }
  }, prefix, " ", formatted);
}
Object.assign(__ds_scope, { MoneyValue });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/MoneyValue.jsx", error: String((e && e.message) || e) }); }

// components/data-display/OddButton.jsx
try { (() => {
/**
 * Odds button used on match panels / bet markets.
 * label = outcome name, odd = multiplier. accent tone by outcome:
 * "team" (blue), "draw" (gold), "selected" (green outline).
 */
function OddButton({
  label,
  odd,
  tone = 'team',
  selected = false,
  disabled = false,
  onClick,
  style
}) {
  const tones = {
    team: 'var(--accent)',
    draw: 'var(--rank-gold)'
  };
  const c = tones[tone] ?? tones.team;
  return /*#__PURE__*/React.createElement("button", {
    onClick: onClick,
    disabled: disabled,
    style: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 4,
      minHeight: 'var(--touch-min)',
      padding: '10px 14px',
      borderRadius: 'var(--radius-md)',
      border: `1.5px solid ${selected ? 'var(--primary)' : 'var(--border-strong)'}`,
      background: selected ? 'rgba(16,185,129,0.12)' : 'var(--surface-inset)',
      color: 'var(--text-primary)',
      cursor: disabled ? 'not-allowed' : 'pointer',
      opacity: disabled ? 0.5 : 1,
      transition: 'border-color var(--dur-fast), background var(--dur-fast)',
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 13,
      fontWeight: 600,
      color: 'var(--text-secondary)'
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    className: "tnum",
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 22,
      color: c
    }
  }, Number(odd).toFixed(2), "x"));
}
Object.assign(__ds_scope, { OddButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/OddButton.jsx", error: String((e && e.message) || e) }); }

// components/data-display/RankRow.jsx
try { (() => {
/**
 * Leaderboard row: rank number + name + trailing value.
 * Top 3 ranks get gold treatment (gold is reserved for rankings/trophies).
 * highlight=true marks the current user (blue).
 */
function RankRow({
  rank,
  name,
  value,
  valueColor = 'var(--money)',
  highlight = false,
  style
}) {
  const isTop = rank <= 3;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 12,
      borderRadius: 'var(--radius-md)',
      background: highlight ? 'var(--accent-muted)' : 'var(--surface-inset)',
      padding: '10px 12px',
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      width: 26,
      height: 26,
      flexShrink: 0,
      borderRadius: '50%',
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 13,
      color: isTop ? 'var(--surface-1)' : 'var(--text-secondary)',
      background: isTop ? 'var(--rank-gold)' : 'transparent'
    }
  }, rank), /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: highlight ? 700 : 600,
      fontSize: 14,
      color: highlight ? 'var(--blue-400)' : 'var(--text-primary)',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap'
    }
  }, name)), /*#__PURE__*/React.createElement("span", {
    className: "tnum",
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 14,
      color: valueColor,
      flexShrink: 0
    }
  }, value));
}
Object.assign(__ds_scope, { RankRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/RankRow.jsx", error: String((e && e.message) || e) }); }

// components/data-display/RoleChip.jsx
try { (() => {
const LABELS = {
  OWNER: 'Dono',
  ADMIN: 'Admin',
  MEMBER: 'Membro'
};

/** Group role chip. One size everywhere; color varies only by role. */
function RoleChip({
  role
}) {
  const privileged = role === 'OWNER' || role === 'ADMIN';
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      borderRadius: 'var(--radius-pill)',
      padding: '2px 8px',
      fontSize: 10,
      fontWeight: 700,
      textTransform: 'uppercase',
      letterSpacing: '0.2em',
      background: privileged ? 'var(--accent-muted)' : 'var(--color-gray-800)',
      color: privileged ? 'var(--blue-400)' : 'var(--text-secondary)'
    }
  }, LABELS[role] ?? 'Sem papel');
}
Object.assign(__ds_scope, { RoleChip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/RoleChip.jsx", error: String((e && e.message) || e) }); }

// components/data-display/SectionHeader.jsx
try { (() => {
/** Uppercase eyebrow label + optional right-aligned action link. */
function SectionHeader({
  label,
  actionLabel,
  onAction,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: 'var(--space-md)',
      ...style
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: {
      margin: 0,
      fontFamily: 'var(--font-sans)',
      fontSize: 'var(--type-eyebrow-size)',
      fontWeight: 700,
      textTransform: 'uppercase',
      letterSpacing: 'var(--type-eyebrow-spacing)',
      color: 'var(--text-muted)'
    }
  }, label), actionLabel && /*#__PURE__*/React.createElement("button", {
    onClick: onAction,
    style: {
      background: 'none',
      border: 'none',
      cursor: 'pointer',
      padding: 0,
      fontSize: 12,
      fontWeight: 600,
      color: 'var(--accent)'
    }
  }, actionLabel));
}
Object.assign(__ds_scope, { SectionHeader });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/SectionHeader.jsx", error: String((e && e.message) || e) }); }

// components/data-display/StatCell.jsx
try { (() => {
/** One cell of the 3-up stat grid (Jogos / Vitórias / Saldo, etc). */
function StatCell({
  label,
  value,
  valueColor = 'var(--text-primary)',
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      borderRadius: 'var(--radius-md)',
      background: 'var(--surface-inset)',
      padding: 'var(--space-md)',
      textAlign: 'center',
      ...style
    }
  }, /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 12,
      fontWeight: 600,
      color: 'var(--text-muted)'
    }
  }, label), /*#__PURE__*/React.createElement("strong", {
    className: "tnum",
    style: {
      display: 'block',
      marginTop: 4,
      fontFamily: 'var(--font-display)',
      fontSize: 18,
      color: valueColor
    }
  }, value));
}
Object.assign(__ds_scope, { StatCell });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/StatCell.jsx", error: String((e && e.message) || e) }); }

// components/data-display/StatusBadge.jsx
try { (() => {
/**
 * Status pill for tournaments and bet slips.
 * tone: "live" (green, pulsing dot), "scheduled" (gray), "ended" (blue),
 *       "won" (green), "lost" (red), "pending" (gray), "warn" (amber).
 */
function StatusBadge({
  tone = 'scheduled',
  children,
  dot,
  style
}) {
  const tones = {
    live: {
      bg: 'var(--color-green-700)',
      fg: '#ecfdf5',
      showDot: true
    },
    scheduled: {
      bg: 'var(--color-gray-700)',
      fg: 'var(--color-gray-200)',
      showDot: false
    },
    ended: {
      bg: 'var(--color-blue-600)',
      fg: '#eff6ff',
      showDot: false
    },
    won: {
      bg: 'var(--color-green-700)',
      fg: '#ecfdf5',
      showDot: false
    },
    lost: {
      bg: 'var(--color-red-700)',
      fg: '#fef2f2',
      showDot: false
    },
    pending: {
      bg: 'var(--color-gray-600)',
      fg: 'var(--color-gray-100)',
      showDot: false
    },
    warn: {
      bg: '#854d0e',
      fg: '#fef9c3',
      showDot: false
    }
  };
  const t = tones[tone] ?? tones.scheduled;
  const showDot = dot ?? t.showDot;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      borderRadius: 'var(--radius-pill)',
      padding: '4px 12px',
      fontSize: 12,
      fontWeight: 700,
      background: t.bg,
      color: t.fg,
      ...style
    }
  }, showDot && /*#__PURE__*/React.createElement("span", {
    style: {
      width: 8,
      height: 8,
      borderRadius: '50%',
      background: 'currentColor',
      animation: 'rb-pulse 1.4s ease-in-out infinite'
    }
  }), children, /*#__PURE__*/React.createElement("style", null, `@keyframes rb-pulse{0%,100%{opacity:1}50%{opacity:0.35}}`));
}
Object.assign(__ds_scope, { StatusBadge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/StatusBadge.jsx", error: String((e && e.message) || e) }); }

// components/feedback/EmptyState.jsx
try { (() => {
/** Zero-data state: dashed panel, icon, title, subtitle, optional CTA slot. */
function EmptyState({
  icon,
  title,
  subtitle,
  children,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      borderRadius: 'var(--radius-lg)',
      border: '1px dashed var(--border-strong)',
      background: 'rgba(17,24,39,0.5)',
      padding: 'var(--space-xl)',
      ...style
    }
  }, icon && /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: icon,
    size: 40,
    color: "var(--color-gray-600)",
    style: {
      marginBottom: 16
    }
  }), /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: 0,
      fontFamily: 'var(--font-display)',
      fontSize: 18,
      fontWeight: 700,
      color: 'var(--text-primary)'
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("p", {
    style: {
      margin: '8px 0 0',
      maxWidth: 360,
      fontSize: 14,
      color: 'var(--text-muted)'
    }
  }, subtitle), children && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 24
    }
  }, children));
}
Object.assign(__ds_scope, { EmptyState });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/EmptyState.jsx", error: String((e && e.message) || e) }); }

// components/feedback/LoadingSkeleton.jsx
try { (() => {
/** Card-shaped shimmer placeholder. rows = number of stacked blocks. */
function LoadingSkeleton({
  rows = 3,
  height = 128,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      ...style
    }
  }, Array.from({
    length: rows
  }).map((_, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      height,
      borderRadius: 'var(--radius-lg)',
      background: 'linear-gradient(90deg, var(--color-gray-800) 25%, #263041 50%, var(--color-gray-800) 75%)',
      backgroundSize: '200% 100%',
      animation: 'rb-shimmer 1.4s ease-in-out infinite'
    }
  })), /*#__PURE__*/React.createElement("style", null, `@keyframes rb-shimmer{0%{background-position:200% 0}100%{background-position:-200% 0}}`));
}
Object.assign(__ds_scope, { LoadingSkeleton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/LoadingSkeleton.jsx", error: String((e && e.message) || e) }); }

// components/feedback/StateBanner.jsx
try { (() => {
/** Inline feedback banner. type: error | warn | info. */
function StateBanner({
  type = 'info',
  message,
  children,
  style
}) {
  const tones = {
    error: {
      border: 'var(--color-red-700)',
      bg: 'rgba(69,10,10,0.6)',
      fg: '#fecaca'
    },
    warn: {
      border: '#854d0e',
      bg: 'rgba(66,32,6,0.6)',
      fg: '#fde68a'
    },
    info: {
      border: 'var(--color-blue-600)',
      bg: 'rgba(24,51,92,0.5)',
      fg: '#bfdbfe'
    }
  };
  const t = tones[type] ?? tones.info;
  return /*#__PURE__*/React.createElement("div", {
    role: type === 'error' ? 'alert' : 'status',
    style: {
      borderRadius: 'var(--radius-md)',
      border: `1px solid ${t.border}`,
      background: t.bg,
      color: t.fg,
      padding: '12px 16px',
      fontSize: 14,
      ...style
    }
  }, message ?? children);
}
Object.assign(__ds_scope, { StateBanner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/StateBanner.jsx", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/auth.jsx
try { (() => {
// Auth screens: Login (name -> PIN) and the no-group full-screen gate.
const DSa = window.ResenhaBETDesignSystem_b0ab9a;
function LoginScreen({
  onLogin
}) {
  const [step, setStep] = React.useState('name');
  const [name, setName] = React.useState('Rafa');
  const [pin, setPin] = React.useState('');
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      minHeight: '100%',
      padding: 24,
      background: 'var(--surface-1)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: 'center',
      marginBottom: 28
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 40,
      color: 'var(--accent)'
    }
  }, "Resenha", /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--primary)'
    }
  }, "BET")), /*#__PURE__*/React.createElement("p", {
    style: {
      color: 'var(--text-muted)',
      marginTop: 4
    }
  }, step === 'name' ? 'Quem é você na resenha?' : 'Digite seu PIN')), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 14
    }
  }, step === 'name' ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(DSa.Input, {
    placeholder: "Seu nome",
    value: name,
    onChange: e => setName(e.target.value)
  }), /*#__PURE__*/React.createElement(DSa.Button, {
    variant: "accent",
    block: true,
    size: "lg",
    disabled: !name.trim(),
    onClick: () => setStep('pin')
  }, "Entrar"), /*#__PURE__*/React.createElement("button", {
    style: {
      background: 'none',
      border: 'none',
      color: 'var(--accent)',
      fontWeight: 600,
      fontSize: 13,
      cursor: 'pointer'
    }
  }, "Criar conta")) : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(DSa.Input, {
    type: "password",
    inputMode: "numeric",
    maxLength: 4,
    placeholder: "\u2022\u2022\u2022\u2022",
    value: pin,
    onChange: e => setPin(e.target.value),
    style: {
      textAlign: 'center',
      fontSize: 24,
      letterSpacing: '0.4em',
      fontWeight: 900
    }
  }), /*#__PURE__*/React.createElement(DSa.Button, {
    variant: "accent",
    block: true,
    size: "lg",
    disabled: pin.length !== 4,
    onClick: onLogin
  }, "Entrar"), /*#__PURE__*/React.createElement("button", {
    onClick: () => setStep('name'),
    style: {
      background: 'none',
      border: 'none',
      color: 'var(--text-muted)',
      fontWeight: 600,
      fontSize: 13,
      cursor: 'pointer'
    }
  }, "Voltar"))));
}
function GroupGate({
  onJoin
}) {
  const [code, setCode] = React.useState('');
  const [err, setErr] = React.useState('');
  const submit = () => {
    if (code === '482913') onJoin();else setErr('Código inválido. Verifique e tente novamente.');
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      minHeight: '100%',
      padding: 24,
      textAlign: 'center',
      background: 'var(--surface-1)'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 30,
      color: 'var(--accent)',
      marginBottom: 20
    }
  }, "ResenhaBET"), /*#__PURE__*/React.createElement("p", {
    style: {
      color: 'var(--text-secondary)',
      maxWidth: 300,
      margin: '0 auto 24px',
      fontSize: 14
    }
  }, "Voc\xEA ainda n\xE3o est\xE1 em um grupo. Insira o c\xF3digo de convite para entrar."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 14,
      maxWidth: 280,
      margin: '0 auto',
      width: '100%'
    }
  }, /*#__PURE__*/React.createElement(DSa.Input, {
    value: code,
    maxLength: 6,
    placeholder: "C\xF3digo",
    invalid: !!err,
    onChange: e => {
      setCode(e.target.value);
      setErr('');
    },
    style: {
      textAlign: 'center',
      fontSize: 20,
      letterSpacing: '0.3em',
      textTransform: 'uppercase'
    }
  }), err && /*#__PURE__*/React.createElement(DSa.StateBanner, {
    type: "error",
    message: err
  }), /*#__PURE__*/React.createElement(DSa.Button, {
    variant: "accent",
    block: true,
    size: "lg",
    disabled: code.length !== 6,
    onClick: submit
  }, "Entrar no Grupo"), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 13,
      color: 'var(--text-muted)'
    }
  }, "\u2014 ou \u2014"), /*#__PURE__*/React.createElement("button", {
    style: {
      background: 'none',
      border: 'none',
      color: 'var(--accent)',
      fontWeight: 600,
      fontSize: 13,
      cursor: 'pointer'
    }
  }, "Criar novo grupo")), /*#__PURE__*/React.createElement("p", {
    style: {
      marginTop: 20,
      fontSize: 11,
      color: 'var(--text-muted)'
    }
  }, "Dica: use o c\xF3digo 482913"));
}
Object.assign(window, {
  LoginScreen,
  GroupGate
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/auth.jsx", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/data.js
try { (() => {
// Mock data for the ResenhaBET mobile UI kit. Portuguese (pt-BR), casual group vibe.
window.RBData = {
  user: {
    name: 'Rafa'
  },
  group: {
    name: 'Resenha da Firma',
    code: '482913',
    memberCount: 8,
    role: 'OWNER'
  },
  wallet: 410.0,
  otherGroups: [{
    name: 'Copa dos Amigos',
    role: 'MEMBER'
  }, {
    name: 'Liga do Trampo',
    role: 'ADMIN'
  }],
  members: [{
    name: 'Rafa',
    role: 'OWNER'
  }, {
    name: 'Duda',
    role: 'ADMIN'
  }, {
    name: 'Zé Carlos',
    role: 'MEMBER'
  }, {
    name: 'Bia',
    role: 'MEMBER'
  }, {
    name: 'Thiago',
    role: 'MEMBER'
  }],
  liveMatch: {
    id: 42,
    tournament: 'Brasileirão da Firma',
    home: {
      name: 'Rafa',
      team: 'Flamengo'
    },
    away: {
      name: 'Zé Carlos',
      team: 'Palmeiras'
    },
    scoreHome: 1,
    scoreAway: 1,
    phase: '2º tempo',
    elapsed: "67'",
    bettingOpen: true,
    isKnockout: false,
    odds: {
      home: 1.85,
      draw: 3.2,
      away: 2.4
    }
  },
  tournaments: [{
    id: 1,
    name: 'Brasileirão da Firma',
    status: 'IN_PROGRESS',
    format: 'Liga + Mata-mata',
    start: '12/06',
    players: 8
  }, {
    id: 2,
    name: 'Copa de Inverno',
    status: 'CREATED',
    format: 'Mata-mata',
    start: '01/07',
    players: 6
  }, {
    id: 3,
    name: 'Liga Relâmpago',
    status: 'ENDED',
    format: 'Liga',
    start: '20/05',
    players: 8
  }],
  bettorRanking: [{
    name: 'Duda',
    wallet: 1240.0
  }, {
    name: 'Bia',
    wallet: 980.5
  }, {
    name: 'Thiago',
    wallet: 640.0
  }, {
    name: 'Zé Carlos',
    wallet: 520.0
  }, {
    name: 'Rafa',
    wallet: 410.0
  }],
  playerRanking: [{
    name: 'Zé Carlos',
    elo: 1284,
    wins: 11,
    matches: 14,
    gd: 18
  }, {
    name: 'Duda',
    elo: 1230,
    wins: 9,
    matches: 14,
    gd: 9
  }, {
    name: 'Rafa',
    elo: 1198,
    wins: 8,
    matches: 14,
    gd: 4
  }, {
    name: 'Bia',
    elo: 1150,
    wins: 6,
    matches: 13,
    gd: -2
  }, {
    name: 'Thiago',
    elo: 1102,
    wins: 4,
    matches: 13,
    gd: -11
  }],
  betSlips: [{
    id: 1,
    createdAt: 'Hoje, 14:32',
    stake: 50,
    combinedOdd: 4.44,
    potentialReturn: 222.0,
    status: 'PENDING',
    tournament: 'Brasileirão da Firma',
    items: [{
      id: 1,
      label: 'Rafa x Zé Carlos',
      outcome: 'Rafa vence',
      odd: 1.85,
      status: 'PENDING'
    }, {
      id: 2,
      label: 'Duda x Bia',
      outcome: 'Empate',
      odd: 2.4,
      status: 'PENDING'
    }]
  }, {
    id: 2,
    createdAt: 'Ontem, 21:10',
    stake: 30,
    combinedOdd: 2.4,
    potentialReturn: 72.0,
    status: 'WON',
    tournament: 'Brasileirão da Firma',
    items: [{
      id: 3,
      label: 'Thiago x Rafa',
      outcome: 'Rafa vence',
      odd: 2.4,
      status: 'WON'
    }]
  }, {
    id: 3,
    createdAt: 'Seg, 19:44',
    stake: 40,
    combinedOdd: 1.65,
    potentialReturn: 66.0,
    status: 'LOST',
    tournament: 'Copa de Inverno',
    items: [{
      id: 4,
      label: 'Bia x Duda',
      outcome: 'Bia vence',
      odd: 1.65,
      status: 'LOST'
    }]
  }]
};
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/data.js", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/screens.jsx
try { (() => {
// Page content for each tab + the match detail view.
const D = window.ResenhaBETDesignSystem_b0ab9a;
const RB = () => window.RBData;
function PageHead({
  eyebrow,
  title,
  subtitle
}) {
  return /*#__PURE__*/React.createElement("header", {
    style: {
      marginBottom: 4
    }
  }, /*#__PURE__*/React.createElement(D.SectionHeader, {
    label: eyebrow
  }), /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 24,
      color: '#fff'
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("p", {
    style: {
      margin: '6px 0 0',
      fontSize: 14,
      color: 'var(--text-muted)'
    }
  }, subtitle));
}
function TeamCol({
  name,
  sub
}) {
  const initials = name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase();
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 4,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: '50%',
      background: 'var(--color-gray-700)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontWeight: 900,
      fontSize: 14,
      color: 'var(--text-secondary)'
    }
  }, initials), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 13,
      fontWeight: 600,
      color: 'var(--text-secondary)'
    }
  }, name), sub && /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 11,
      color: 'var(--text-muted)'
    }
  }, sub));
}

// ---------- Dashboard ----------
function Dashboard({
  onOpenMatch
}) {
  const d = RB();
  const m = d.liveMatch;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 22
    }
  }, /*#__PURE__*/React.createElement(PageHead, {
    eyebrow: "Dashboard",
    title: "Resumo",
    subtitle: "Bem vindo ao ResenhaBET."
  }), /*#__PURE__*/React.createElement(D.Card, {
    padding: "14px",
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 11,
      textTransform: 'uppercase',
      letterSpacing: '0.08em',
      color: 'var(--text-muted)',
      fontWeight: 700
    }
  }, "Sua carteira"), /*#__PURE__*/React.createElement(D.MoneyValue, {
    amount: d.wallet,
    size: 26
  })), /*#__PURE__*/React.createElement(D.Icon, {
    name: "account_balance_wallet",
    size: 30,
    color: "var(--primary)"
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 8,
      height: 8,
      borderRadius: '50%',
      background: 'var(--status-live)',
      display: 'inline-block'
    }
  }), /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: 0,
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 18
    }
  }, "Ao Vivo Agora")), /*#__PURE__*/React.createElement("button", {
    onClick: onOpenMatch,
    style: {
      display: 'block',
      width: '100%',
      textAlign: 'left',
      padding: 0,
      border: 'none',
      background: 'none',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement(D.Card, {
    padding: "16px"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      fontSize: 12,
      color: 'var(--text-muted)',
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement("span", null, m.tournament), /*#__PURE__*/React.createElement(D.StatusBadge, {
    tone: "live"
  }, "Ao Vivo")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr auto 1fr',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(TeamCol, {
    name: m.home.name,
    sub: m.home.team
  }), /*#__PURE__*/React.createElement("div", {
    className: "tnum",
    style: {
      display: 'flex',
      gap: 8,
      alignItems: 'center',
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 26,
      background: 'var(--surface-inset)',
      borderRadius: 10,
      padding: '6px 12px'
    }
  }, /*#__PURE__*/React.createElement("span", null, m.scoreHome), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 14,
      color: 'var(--text-muted)'
    }
  }, "\xD7"), /*#__PURE__*/React.createElement("span", null, m.scoreAway)), /*#__PURE__*/React.createElement(TeamCol, {
    name: m.away.name,
    sub: m.away.team
  }))))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: '0 0 12px',
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 18
    }
  }, "Torneios Ativos"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, d.tournaments.filter(t => t.status !== 'ENDED').map(t => /*#__PURE__*/React.createElement(D.Card, {
    key: t.id,
    padding: "16px"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'flex-start',
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("h4", {
    style: {
      margin: 0,
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 17
    }
  }, t.name), /*#__PURE__*/React.createElement(TournamentStatus, {
    status: t.status
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(InfoRow, {
    label: "Formato",
    value: t.format
  }), /*#__PURE__*/React.createElement(InfoRow, {
    label: "In\xEDcio",
    value: t.start
  }), /*#__PURE__*/React.createElement(InfoRow, {
    label: "Inscritos",
    value: `${t.players} jogadores`
  })))))));
}
function InfoRow({
  label,
  value
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      fontSize: 13,
      background: 'var(--surface-inset)',
      borderRadius: 8,
      padding: '8px 12px'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--text-muted)'
    }
  }, label), /*#__PURE__*/React.createElement("strong", {
    style: {
      color: 'var(--text-secondary)'
    }
  }, value));
}
function TournamentStatus({
  status
}) {
  const map = {
    IN_PROGRESS: ['live', 'Em andamento'],
    CREATED: ['scheduled', 'Agendado'],
    ENDED: ['ended', 'Encerrado']
  };
  const [tone, label] = map[status] || map.CREATED;
  return /*#__PURE__*/React.createElement(D.StatusBadge, {
    tone: tone
  }, label);
}

// ---------- Tournaments ----------
function Tournaments() {
  const d = RB();
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 18
    }
  }, /*#__PURE__*/React.createElement(PageHead, {
    eyebrow: "Torneios",
    title: "Torneios",
    subtitle: "Todos os campeonatos do grupo"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, d.tournaments.map(t => /*#__PURE__*/React.createElement(D.Card, {
    key: t.id,
    padding: "16px"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'flex-start',
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h4", {
    style: {
      margin: 0,
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 17
    }
  }, t.name), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: '4px 0 0',
      fontSize: 12,
      color: 'var(--text-muted)'
    }
  }, t.format, " \xB7 ", t.players, " jogadores")), /*#__PURE__*/React.createElement(TournamentStatus, {
    status: t.status
  }))))));
}

// ---------- Groups (in-page tabs) ----------
function Groups() {
  const d = RB();
  const [tab, setTab] = React.useState('overview');
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 18
    }
  }, /*#__PURE__*/React.createElement(PageHead, {
    eyebrow: "Grupos",
    title: "Grupos"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      background: 'var(--surface-3)',
      borderRadius: 'var(--radius-md)',
      padding: 4,
      gap: 4
    }
  }, [['overview', 'Overview'], ['members', 'Membros'], ['settings', 'Config']].map(([id, label]) => /*#__PURE__*/React.createElement("button", {
    key: id,
    onClick: () => setTab(id),
    style: {
      flex: 1,
      minHeight: 40,
      border: 'none',
      borderRadius: 'var(--radius-sm)',
      cursor: 'pointer',
      fontWeight: 700,
      fontSize: 13,
      background: tab === id ? 'var(--accent)' : 'transparent',
      color: tab === id ? '#fff' : 'var(--text-muted)'
    }
  }, label))), tab === 'overview' && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(D.Card, {
    elevated: true,
    padding: "16px"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'flex-start'
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 11,
      textTransform: 'uppercase',
      letterSpacing: '0.08em',
      color: 'var(--text-muted)',
      fontWeight: 700
    }
  }, "Grupo ativo"), /*#__PURE__*/React.createElement("h4", {
    style: {
      margin: '4px 0',
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 18
    }
  }, d.group.name), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 13,
      color: 'var(--text-muted)'
    }
  }, d.group.memberCount, " membros \xB7 c\xF3digo ", d.group.code)), /*#__PURE__*/React.createElement(D.RoleChip, {
    role: d.group.role
  }))), /*#__PURE__*/React.createElement(D.SectionHeader, {
    label: "Outros grupos"
  }), d.otherGroups.map(g => /*#__PURE__*/React.createElement(D.Card, {
    key: g.name,
    padding: "14px",
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: 600
    }
  }, g.name), /*#__PURE__*/React.createElement(D.RoleChip, {
    role: g.role
  })))), tab === 'members' && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8
    }
  }, d.members.map(m => /*#__PURE__*/React.createElement(D.Card, {
    key: m.name,
    padding: "12px",
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: 10,
      background: 'var(--accent-muted)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontWeight: 900,
      color: 'var(--blue-400)'
    }
  }, m.name[0]), /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: 600
    }
  }, m.name)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(D.RoleChip, {
    role: m.role
  }), /*#__PURE__*/React.createElement(D.Icon, {
    name: "more_vert",
    size: 20,
    color: "var(--text-muted)"
  }))))), tab === 'settings' && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 14
    }
  }, /*#__PURE__*/React.createElement(D.Card, {
    padding: "16px"
  }, /*#__PURE__*/React.createElement(D.SectionHeader, {
    label: "Nome do grupo"
  }), /*#__PURE__*/React.createElement(D.Input, {
    defaultValue: d.group.name
  })), /*#__PURE__*/React.createElement(D.Card, {
    padding: "16px"
  }, /*#__PURE__*/React.createElement(D.SectionHeader, {
    label: "Zona de perigo"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(D.Button, {
    variant: "secondary",
    block: true
  }, "Sair do grupo"), /*#__PURE__*/React.createElement(D.Button, {
    variant: "destructive",
    block: true
  }, "Excluir grupo")))));
}

// ---------- My Bets (bets + ranking segment) ----------
function MyBets() {
  const d = RB();
  const [seg, setSeg] = React.useState('bets');
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 18
    }
  }, /*#__PURE__*/React.createElement(PageHead, {
    eyebrow: "Apostas",
    title: "Minhas Apostas",
    subtitle: "Hist\xF3rico de apostas realizadas"
  }), /*#__PURE__*/React.createElement(Segment, {
    value: seg,
    onChange: setSeg,
    options: [['bets', 'Meus bilhetes'], ['ranking', 'Ranking']]
  }), seg === 'bets' ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, d.betSlips.map(s => /*#__PURE__*/React.createElement(BetSlipCard, {
    key: s.id,
    slip: s
  }))) : /*#__PURE__*/React.createElement(D.Card, {
    padding: "16px"
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: '0 0 12px',
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 18
    }
  }, "Ranking de Apostadores"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 6
    }
  }, d.bettorRanking.map((b, i) => /*#__PURE__*/React.createElement(D.RankRow, {
    key: b.name,
    rank: i + 1,
    name: b.name,
    highlight: b.name === d.user.name,
    value: `R$ ${b.wallet.toLocaleString('pt-BR', {
      minimumFractionDigits: 2
    })}`
  })))));
}
function Segment({
  value,
  onChange,
  options
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      background: 'var(--surface-3)',
      borderRadius: 'var(--radius-md)',
      padding: 4,
      gap: 4
    }
  }, options.map(([id, label]) => /*#__PURE__*/React.createElement("button", {
    key: id,
    onClick: () => onChange(id),
    style: {
      flex: 1,
      minHeight: 38,
      border: 'none',
      borderRadius: 'var(--radius-sm)',
      cursor: 'pointer',
      fontWeight: 700,
      fontSize: 13,
      background: value === id ? 'var(--surface-2)' : 'transparent',
      color: value === id ? 'var(--text-primary)' : 'var(--text-muted)'
    }
  }, label)));
}
function BetSlipCard({
  slip
}) {
  const map = {
    PENDING: ['pending', 'Pendente'],
    WON: ['won', 'Ganhou'],
    LOST: ['lost', 'Perdeu']
  };
  const [tone, label] = map[slip.status];
  return /*#__PURE__*/React.createElement(D.Card, {
    padding: "16px"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'flex-start',
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: 12,
      color: 'var(--text-muted)'
    }
  }, slip.createdAt), /*#__PURE__*/React.createElement(D.MoneyValue, {
    amount: slip.stake,
    tone: "neutral",
    size: 18
  }), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: '2px 0 0',
      fontSize: 10,
      textTransform: 'uppercase',
      letterSpacing: '0.15em',
      color: 'var(--text-muted)'
    }
  }, slip.tournament)), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: 'right'
    }
  }, /*#__PURE__*/React.createElement(D.StatusBadge, {
    tone: tone
  }, label), slip.status === 'WON' && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 6
    }
  }, /*#__PURE__*/React.createElement(D.MoneyValue, {
    amount: slip.potentialReturn,
    tone: "positive",
    size: 13
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: 'repeat(3,1fr)',
      gap: 8,
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement(D.StatCell, {
    label: "Odd comb.",
    value: `${slip.combinedOdd}x`
  }), /*#__PURE__*/React.createElement(D.StatCell, {
    label: "Retorno",
    value: `R$ ${slip.potentialReturn.toFixed(0)}`,
    valueColor: "var(--money)"
  }), /*#__PURE__*/React.createElement(D.StatCell, {
    label: "Itens",
    value: slip.items.length
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
      borderTop: '1px solid var(--border-hairline)',
      paddingTop: 12
    }
  }, slip.items.map(it => {
    const [t, l] = map[it.status];
    return /*#__PURE__*/React.createElement("div", {
      key: it.id,
      style: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        background: 'var(--surface-inset)',
        borderRadius: 10,
        padding: 10
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("p", {
      style: {
        margin: 0,
        fontWeight: 600,
        fontSize: 13
      }
    }, it.label), /*#__PURE__*/React.createElement("p", {
      style: {
        margin: '2px 0 0',
        fontSize: 11,
        color: 'var(--text-muted)'
      }
    }, it.outcome, " \xB7 ", it.odd, "x")), /*#__PURE__*/React.createElement(D.StatusBadge, {
      tone: t,
      style: {
        fontSize: 10,
        padding: '2px 8px'
      }
    }, l));
  })));
}

// ---------- Players (list + Elo leaderboard) ----------
function Players() {
  const d = RB();
  const [seg, setSeg] = React.useState('list');
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 18
    }
  }, /*#__PURE__*/React.createElement(PageHead, {
    eyebrow: "Jogadores",
    title: "Jogadores",
    subtitle: `Grupo ativo: ${d.group.name}`
  }), /*#__PURE__*/React.createElement(Segment, {
    value: seg,
    onChange: setSeg,
    options: [['list', 'Lista'], ['ranking', 'Ranking Elo']]
  }), seg === 'list' ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, d.playerRanking.map(p => /*#__PURE__*/React.createElement(D.Card, {
    key: p.name,
    padding: "16px"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 14,
      alignItems: 'center',
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 52,
      height: 52,
      borderRadius: 12,
      background: 'var(--accent-muted)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontWeight: 900,
      fontSize: 18,
      color: 'var(--blue-400)'
    }
  }, p.name[0]), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      fontWeight: 700,
      fontSize: 16
    }
  }, p.name), /*#__PURE__*/React.createElement("span", {
    style: {
      display: 'inline-block',
      marginTop: 4,
      borderRadius: 999,
      background: 'var(--color-green-700)',
      color: '#ecfdf5',
      padding: '2px 10px',
      fontSize: 11,
      fontWeight: 700
    }
  }, "ativo"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: 'repeat(3,1fr)',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(D.StatCell, {
    label: "Jogos",
    value: p.matches
  }), /*#__PURE__*/React.createElement(D.StatCell, {
    label: "Vit\xF3rias",
    value: p.wins
  }), /*#__PURE__*/React.createElement(D.StatCell, {
    label: "Saldo",
    value: p.gd > 0 ? `+${p.gd}` : p.gd,
    valueColor: p.gd >= 0 ? 'var(--money-positive)' : 'var(--danger)'
  }))))) : /*#__PURE__*/React.createElement(D.Card, {
    padding: "16px"
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: '0 0 12px',
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 18
    }
  }, "Ranking Elo"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 6
    }
  }, d.playerRanking.map((p, i) => /*#__PURE__*/React.createElement(D.RankRow, {
    key: p.name,
    rank: i + 1,
    name: p.name,
    highlight: p.name === d.user.name,
    value: p.elo,
    valueColor: "var(--rank-gold)"
  })))));
}

// ---------- Match detail ----------
function MatchDetail({
  onBack,
  onAddToSlip
}) {
  const m = RB().liveMatch;
  const [picked, setPicked] = React.useState(null);
  const pick = (key, label, odd) => {
    setPicked(key);
    onAddToSlip({
      eventId: m.id,
      label: `${m.home.name} x ${m.away.name}`,
      outcome: label,
      odd
    });
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 18
    }
  }, /*#__PURE__*/React.createElement("button", {
    onClick: onBack,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      background: 'none',
      border: 'none',
      color: 'var(--accent)',
      fontWeight: 600,
      cursor: 'pointer',
      padding: 0
    }
  }, /*#__PURE__*/React.createElement(D.Icon, {
    name: "arrow_back",
    size: 18
  }), " Voltar"), /*#__PURE__*/React.createElement(D.Card, {
    padding: "16px"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      fontSize: 12,
      color: 'var(--text-muted)',
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("span", null, m.tournament), /*#__PURE__*/React.createElement(D.StatusBadge, {
    tone: "live"
  }, "Ao Vivo \xB7 ", m.elapsed)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr auto 1fr',
      alignItems: 'center',
      gap: 8,
      marginBottom: 16
    }
  }, /*#__PURE__*/React.createElement(TeamCol, {
    name: m.home.name,
    sub: m.home.team
  }), /*#__PURE__*/React.createElement("div", {
    className: "tnum",
    style: {
      display: 'flex',
      gap: 10,
      alignItems: 'center',
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 34
    }
  }, /*#__PURE__*/React.createElement("span", null, m.scoreHome), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 16,
      color: 'var(--text-muted)'
    }
  }, "\xD7"), /*#__PURE__*/React.createElement("span", null, m.scoreAway)), /*#__PURE__*/React.createElement(TeamCol, {
    name: m.away.name,
    sub: m.away.team
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: 'center',
      color: 'var(--status-live)',
      fontWeight: 700,
      fontSize: 13,
      marginBottom: 12
    }
  }, "APOSTAS ABERTAS!"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 1fr 1fr',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(D.OddButton, {
    label: m.home.name,
    odd: m.odds.home,
    selected: picked === 'home',
    onClick: () => pick('home', `${m.home.name} vence`, m.odds.home)
  }), /*#__PURE__*/React.createElement(D.OddButton, {
    label: "Empate",
    odd: m.odds.draw,
    tone: "draw",
    selected: picked === 'draw',
    onClick: () => pick('draw', 'Empate', m.odds.draw)
  }), /*#__PURE__*/React.createElement(D.OddButton, {
    label: m.away.name,
    odd: m.odds.away,
    selected: picked === 'away',
    onClick: () => pick('away', `${m.away.name} vence`, m.odds.away)
  }))));
}
Object.assign(window, {
  Dashboard,
  Tournaments,
  Groups,
  MyBets,
  Players,
  MatchDetail
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/screens.jsx", error: String((e && e.message) || e) }); }

// ui_kits/mobile-app/shell.jsx
try { (() => {
// Shell: top header + bottom tab bar + FAB + bottom-sheet bet slip.
const DS = window.ResenhaBETDesignSystem_b0ab9a;
const TABS = [{
  id: 'dashboard',
  icon: 'home',
  label: 'Dashboard'
}, {
  id: 'tournaments',
  icon: 'emoji_events',
  label: 'Torneios'
}, {
  id: 'groups',
  icon: 'group',
  label: 'Grupos'
}, {
  id: 'bets',
  icon: 'receipt_long',
  label: 'Apostas'
}, {
  id: 'players',
  icon: 'person',
  label: 'Jogadores'
}];
function Header({
  groupName,
  onGroupTap
}) {
  return /*#__PURE__*/React.createElement("header", {
    style: {
      height: 'var(--header-height)',
      flexShrink: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 16px',
      background: 'var(--surface-2)',
      borderBottom: '1px solid var(--border-hairline)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 900,
      fontSize: 18,
      color: 'var(--accent)'
    }
  }, "Resenha", /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--primary)'
    }
  }, "BET")), /*#__PURE__*/React.createElement("button", {
    onClick: onGroupTap,
    style: {
      display: 'inline-flex',
      alignItems: 'center',
      gap: 6,
      minHeight: 36,
      padding: '0 12px',
      borderRadius: 'var(--radius-pill)',
      background: 'var(--surface-3)',
      border: '1px solid var(--border-hairline)',
      color: 'var(--text-secondary)',
      fontSize: 13,
      fontWeight: 600,
      cursor: 'pointer',
      maxWidth: 170
    }
  }, /*#__PURE__*/React.createElement(DS.Icon, {
    name: "group",
    size: 16,
    color: "var(--accent)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap'
    }
  }, groupName), /*#__PURE__*/React.createElement(DS.Icon, {
    name: "expand_more",
    size: 16,
    color: "var(--text-muted)"
  })));
}
function TabBar({
  active,
  onChange
}) {
  return /*#__PURE__*/React.createElement("nav", {
    style: {
      height: 'var(--tab-bar-height)',
      flexShrink: 0,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-around',
      background: 'var(--surface-2)',
      borderTop: '1px solid var(--border-hairline)',
      boxShadow: 'var(--shadow-bottom-nav)'
    }
  }, TABS.map(t => {
    const on = active === t.id;
    return /*#__PURE__*/React.createElement("button", {
      key: t.id,
      onClick: () => onChange(t.id),
      style: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 3,
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        minWidth: 44,
        padding: '4px 0',
        color: on ? 'var(--accent)' : 'var(--text-muted)'
      }
    }, /*#__PURE__*/React.createElement(DS.Icon, {
      name: t.icon,
      size: 22
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 10,
        fontWeight: 700
      }
    }, t.label));
  }));
}

// Bottom-sheet bet slip
function BetSlipSheet({
  entries,
  onClose,
  onRemove,
  stake,
  setStake,
  onPlace,
  walletBalance
}) {
  const combined = entries.reduce((a, e) => a * e.odd, 1);
  const potential = stake > 0 ? stake * combined : 0;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      inset: 0,
      zIndex: 60,
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'flex-end'
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: 'absolute',
      inset: 0,
      background: 'rgba(0,0,0,0.6)'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'relative',
      background: 'var(--surface-2)',
      borderTopLeftRadius: 20,
      borderTopRightRadius: 20,
      borderTop: '1px solid var(--border-hairline)',
      boxShadow: 'var(--shadow-bottom-sheet)',
      padding: 16,
      maxHeight: '80%',
      overflowY: 'auto'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(DS.Icon, {
    name: "shopping_cart",
    size: 20,
    color: "var(--accent)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: 'var(--font-display)',
      fontWeight: 700,
      fontSize: 17
    }
  }, "Carrinho de Apostas")), /*#__PURE__*/React.createElement("button", {
    onClick: onClose,
    style: {
      background: 'none',
      border: 'none',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement(DS.Icon, {
    name: "expand_more",
    size: 22,
    color: "var(--text-muted)"
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 8
    }
  }, entries.map(e => /*#__PURE__*/React.createElement("div", {
    key: e.eventId,
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 10,
      borderRadius: 'var(--radius-md)',
      background: 'var(--surface-inset)',
      padding: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontWeight: 600,
      fontSize: 14,
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap'
    }
  }, e.label), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: '2px 0 0',
      fontSize: 12,
      color: 'var(--text-muted)'
    }
  }, e.outcome, " \u2014 ", e.odd, "x")), /*#__PURE__*/React.createElement("button", {
    onClick: () => onRemove(e.eventId),
    style: {
      background: 'none',
      border: 'none',
      cursor: 'pointer',
      color: 'var(--text-muted)'
    }
  }, /*#__PURE__*/React.createElement(DS.Icon, {
    name: "close",
    size: 18
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      marginTop: 16,
      fontSize: 14
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--text-muted)'
    }
  }, "Odd combinada"), /*#__PURE__*/React.createElement("span", {
    className: "tnum",
    style: {
      fontWeight: 700,
      color: 'var(--accent)'
    }
  }, combined.toFixed(2), "x")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      marginBottom: 8,
      fontSize: 13
    }
  }, /*#__PURE__*/React.createElement("label", {
    style: {
      fontWeight: 600,
      color: 'var(--text-secondary)'
    }
  }, "Valor da aposta"), /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--text-muted)'
    }
  }, "Saldo: R$ ", walletBalance.toFixed(2))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(DS.Input, {
    type: "number",
    value: stake || '',
    onChange: e => setStake(Number(e.target.value)),
    placeholder: "0",
    style: {
      textAlign: 'center',
      fontSize: 18
    }
  }), /*#__PURE__*/React.createElement(DS.Button, {
    variant: "secondary",
    onClick: () => setStake(walletBalance)
  }, "MAX"))), stake > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      marginTop: 12,
      fontSize: 14
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: 'var(--text-muted)'
    }
  }, "Retorno potencial"), /*#__PURE__*/React.createElement(DS.MoneyValue, {
    amount: potential,
    tone: "positive"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 10,
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement(DS.Button, {
    variant: "primary",
    block: true,
    disabled: !stake || entries.length === 0,
    onClick: onPlace
  }, "Apostar"))));
}
Object.assign(window, {
  Header,
  TabBar,
  BetSlipSheet,
  RB_TABS: TABS
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/mobile-app/shell.jsx", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Card = __ds_scope.Card;

__ds_ns.Fab = __ds_scope.Fab;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.Input = __ds_scope.Input;

__ds_ns.MoneyValue = __ds_scope.MoneyValue;

__ds_ns.OddButton = __ds_scope.OddButton;

__ds_ns.RankRow = __ds_scope.RankRow;

__ds_ns.RoleChip = __ds_scope.RoleChip;

__ds_ns.SectionHeader = __ds_scope.SectionHeader;

__ds_ns.StatCell = __ds_scope.StatCell;

__ds_ns.StatusBadge = __ds_scope.StatusBadge;

__ds_ns.EmptyState = __ds_scope.EmptyState;

__ds_ns.LoadingSkeleton = __ds_scope.LoadingSkeleton;

__ds_ns.StateBanner = __ds_scope.StateBanner;

})();
