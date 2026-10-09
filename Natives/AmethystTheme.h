#import <UIKit/UIKit.h>

// Launcher look: dark slate surfaces, an emerald accent and rounded type.
// Everything visual the launcher shares goes through here so it stays consistent.

UIColor *AMThemeAccent(void);
UIColor *AMThemeBackground(void);
UIColor *AMThemeSurface(void);
UIColor *AMThemeSurfaceRaised(void);
UIFont *AMThemeRoundedFont(CGFloat size, UIFontWeight weight);

// Applies appearance proxies and the window-wide tint. Call before the first
// view controller is shown.
void AMThemeApply(UIWindow *window);

// Gives a primary action button the capsule, accent-filled treatment.
void AMThemeStylePrimaryButton(UIButton *button);
