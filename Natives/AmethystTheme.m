#import "AmethystTheme.h"

static UIColor *AMRGB(CGFloat r, CGFloat g, CGFloat b) {
    return [UIColor colorWithRed:r / 255.0 green:g / 255.0 blue:b / 255.0 alpha:1.0];
}

UIColor *AMThemeAccent(void) {
    return AMRGB(52, 211, 153);
}

UIColor *AMThemeBackground(void) {
    return AMRGB(13, 17, 23);
}

UIColor *AMThemeSurface(void) {
    return AMRGB(22, 27, 34);
}

UIColor *AMThemeSurfaceRaised(void) {
    return AMRGB(33, 40, 50);
}

UIFont *AMThemeRoundedFont(CGFloat size, UIFontWeight weight) {
    UIFont *font = [UIFont systemFontOfSize:size weight:weight];
    UIFontDescriptor *rounded = [font.fontDescriptor fontDescriptorWithDesign:UIFontDescriptorSystemDesignRounded];
    return rounded ? [UIFont fontWithDescriptor:rounded size:size] : font;
}

void AMThemeApply(UIWindow *window) {
    window.overrideUserInterfaceStyle = UIUserInterfaceStyleDark;
    window.tintColor = AMThemeAccent();
    window.backgroundColor = AMThemeBackground();

    UINavigationBarAppearance *nav = [UINavigationBarAppearance new];
    [nav configureWithDefaultBackground];
    nav.backgroundColor = [AMThemeBackground() colorWithAlphaComponent:0.85];
    nav.shadowColor = UIColor.clearColor;
    nav.titleTextAttributes = @{
        NSFontAttributeName: AMThemeRoundedFont(18, UIFontWeightBold),
        NSForegroundColorAttributeName: UIColor.whiteColor
    };
    nav.largeTitleTextAttributes = @{
        NSFontAttributeName: AMThemeRoundedFont(34, UIFontWeightHeavy),
        NSForegroundColorAttributeName: UIColor.whiteColor
    };
    UINavigationBar.appearance.standardAppearance = nav;
    UINavigationBar.appearance.compactAppearance = nav;
    UINavigationBar.appearance.scrollEdgeAppearance = nav;

    UIToolbarAppearance *toolbar = [UIToolbarAppearance new];
    [toolbar configureWithDefaultBackground];
    toolbar.backgroundColor = [AMThemeSurface() colorWithAlphaComponent:0.9];
    toolbar.shadowColor = UIColor.clearColor;
    UIToolbar.appearance.standardAppearance = toolbar;
    UIToolbar.appearance.compactAppearance = toolbar;
    if (@available(iOS 15.0, *)) {
        UIToolbar.appearance.scrollEdgeAppearance = toolbar;
    }

    UITableView.appearance.backgroundColor = AMThemeBackground();
    UITableView.appearance.separatorColor = [UIColor colorWithWhite:1.0 alpha:0.06];
    UITableViewCell.appearance.backgroundColor = AMThemeSurface();
    UICollectionView.appearance.backgroundColor = AMThemeBackground();

    UISwitch.appearance.onTintColor = AMThemeAccent();
    UISlider.appearance.minimumTrackTintColor = AMThemeAccent();
    UIProgressView.appearance.progressTintColor = AMThemeAccent();
    UISegmentedControl.appearance.selectedSegmentTintColor = AMThemeAccent();
    [UISegmentedControl.appearance setTitleTextAttributes:@{
        NSForegroundColorAttributeName: AMThemeBackground(),
        NSFontAttributeName: AMThemeRoundedFont(13, UIFontWeightSemibold)
    } forState:UIControlStateSelected];
}

void AMThemeStylePrimaryButton(UIButton *button) {
    button.backgroundColor = AMThemeAccent();
    button.tintColor = AMThemeBackground();
    [button setTitleColor:AMThemeBackground() forState:UIControlStateNormal];
    [button setTitleColor:[AMThemeBackground() colorWithAlphaComponent:0.5] forState:UIControlStateDisabled];
    button.titleLabel.font = AMThemeRoundedFont(17, UIFontWeightHeavy);
    button.layer.cornerRadius = button.bounds.size.height / 2;
    if (@available(iOS 13.0, *)) {
        button.layer.cornerCurve = kCACornerCurveContinuous;
    }
    button.layer.shadowColor = AMThemeAccent().CGColor;
    button.layer.shadowOpacity = 0.35;
    button.layer.shadowRadius = 10;
    button.layer.shadowOffset = CGSizeMake(0, 3);
}
