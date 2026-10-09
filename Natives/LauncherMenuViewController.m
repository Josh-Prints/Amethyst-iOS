#import "authenticator/BaseAuthenticator.h"
#import "AccountListViewController.h"
#import "AFNetworking.h"
#import "ALTServerConnection.h"
#import "LauncherNavigationController.h"
#import "LauncherMenuViewController.h"
#import "LauncherNewsViewController.h"
#import "LauncherPreferences.h"
#import "LauncherPreferencesViewController.h"
#import "LauncherProfilesViewController.h"
#import "PLProfiles.h"
#import "UIButton+AFNetworking.h"
#import "UIImageView+AFNetworking.h"
#import "UIKit+hook.h"
#import "AmethystTheme.h"
#import "config.h"
#import "ios_uikit_bridge.h"
#import "utils.h"

#include <dlfcn.h>

static NSString *AmethystBuildVersion(void) {
    return NSBundle.mainBundle.infoDictionary[@"CFBundleShortVersionString"];
}

@implementation LauncherMenuCustomItem

+ (LauncherMenuCustomItem *)title:(NSString *)title imageName:(NSString *)imageName action:(id)action {
    LauncherMenuCustomItem *item = [[LauncherMenuCustomItem alloc] init];
    item.title = title;
    item.imageName = imageName;
    item.action = action;
    return item;
}

+ (LauncherMenuCustomItem *)vcClass:(Class)class {
    id vc = [class new];
    LauncherMenuCustomItem *item = [[LauncherMenuCustomItem alloc] init];
    item.title = [vc title];
    item.imageName = [vc imageName];
    // View controllers are put into an array to keep its state
    item.vcArray = @[vc];
    return item;
}

@end

@interface LauncherMenuViewController()
@property(nonatomic) NSMutableArray<LauncherMenuCustomItem*> *options;
@property(nonatomic) UILabel *statusLabel;
@property(nonatomic) int lastSelectedIndex;
@end

@implementation LauncherMenuViewController

- (instancetype)init {
    return [super initWithStyle:UITableViewStyleInsetGrouped];
}

#define contentNavigationController ((LauncherNavigationController *)self.splitViewController.viewControllers[1])

- (void)viewDidLoad {
    [super viewDidLoad];
    
    self.isInitialVc = YES;
    
    UIView *titleView = [self buildHeaderView];
    self.tableView.tableHeaderView = titleView;
    self.navigationItem.title = @"";

    LauncherMenuCustomItem *homeItem = [LauncherMenuCustomItem vcClass:LauncherProfilesViewController.class];
    homeItem.imageName = @"square.stack.3d.up.fill";
    LauncherMenuCustomItem *newsItem = [LauncherMenuCustomItem vcClass:LauncherNewsViewController.class];
    newsItem.imageName = @"newspaper.fill";
    LauncherMenuCustomItem *settingsItem = [LauncherMenuCustomItem vcClass:LauncherPreferencesViewController.class];
    settingsItem.imageName = @"gearshape.fill";
    self.options = @[homeItem, newsItem, settingsItem].mutableCopy;
    if (realUIIdiom != UIUserInterfaceIdiomTV) {
        [self.options addObject:(id)[LauncherMenuCustomItem
                                     title:localize(@"launcher.menu.custom_controls", nil)
                                     imageName:@"gamecontroller.fill" action:^{
            [contentNavigationController performSelector:@selector(enterCustomControls)];
        }]];
    }
    [self.options addObject:
     (id)[LauncherMenuCustomItem
          title:localize(@"launcher.menu.execute_jar", nil)
          imageName:@"shippingbox.fill" action:^{
        [contentNavigationController performSelector:@selector(enterModInstaller)];
    }]];
    
    // TODO: Finish log-uploading service integration
    [self.options addObject:
     (id)[LauncherMenuCustomItem
          title:localize(@"login.menu.sendlogs", nil)
          imageName:@"doc.text.magnifyingglass" action:^{
        NSString *latestlogPath = [NSString stringWithFormat:@"file://%s/latestlog.old.txt", getenv("POJAV_HOME")];
        NSLog(@"Path is %@", latestlogPath);
        UIActivityViewController *activityVC;
        if (realUIIdiom != UIUserInterfaceIdiomTV) {
            activityVC = [[UIActivityViewController alloc]
                          initWithActivityItems:@[[NSURL URLWithString:latestlogPath]]
                          applicationActivities:nil];
        } else {
            dlopen("/System/Library/PrivateFrameworks/SharingUI.framework/SharingUI", RTLD_GLOBAL);
            activityVC =
            [[NSClassFromString(@"SFAirDropSharingViewControllerTV") alloc]
             performSelector:@selector(initWithSharingItems:)
             withObject:@[[NSURL URLWithString:latestlogPath]]];
        }
        activityVC.popoverPresentationController.sourceView = titleView;
        activityVC.popoverPresentationController.sourceRect = titleView.bounds;
        [self presentViewController:activityVC animated:YES completion:nil];
    }]];
    
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    dateFormatter.dateFormat = @"MM-dd";
    NSString* date = [dateFormatter stringFromDate:NSDate.date];
    if([date isEqualToString:@"06-29"] || [date isEqualToString:@"06-30"] || [date isEqualToString:@"07-01"]) {
        [self.options addObject:(id)[LauncherMenuCustomItem
                                     title:@"Technoblade never dies!"
                                     imageName:@"" action:^{
            openLink(self, [NSURL URLWithString:@"https://youtu.be/DPMluEVUqS0"]);
        }]];
    }
    
    self.tableView.backgroundColor = AMThemeBackground();
    self.tableView.rowHeight = 50;
    self.tableView.separatorColor = [UIColor colorWithWhite:1.0 alpha:0.06];

    // Show the build right in the sidebar so a fresh install is obvious at a glance
    UILabel *buildLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, 0, 44)];
    buildLabel.numberOfLines = 2;
    buildLabel.frame = CGRectMake(0, 0, 0, 56);
    buildLabel.text = [NSString stringWithFormat:@"Emerald %@ · %s\nBased on Amethyst & PojavLauncher", AmethystBuildVersion(), CONFIG_COMMIT];
    buildLabel.textAlignment = NSTextAlignmentCenter;
    buildLabel.font = AMThemeRoundedFont(13, UIFontWeightSemibold);
    buildLabel.textColor = [AMThemeAccent() colorWithAlphaComponent:0.8];
    self.tableView.tableFooterView = buildLabel;
    
    self.navigationController.toolbarHidden = NO;
    UIActivityIndicatorViewStyle indicatorStyle = UIActivityIndicatorViewStyleMedium;
    UIActivityIndicatorView *toolbarIndicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:indicatorStyle];
    [toolbarIndicator startAnimating];
    self.toolbarItems = @[
        [[UIBarButtonItem alloc] initWithCustomView:toolbarIndicator],
        [[UIBarButtonItem alloc] init]
    ];
    self.toolbarItems[1].tintColor = UIColor.labelColor;
    
    // Setup the account button
    self.accountBtnItem = [self drawAccountButton];
    
    [self updateAccountInfo];
    
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:0 inSection:0];
    [self.tableView selectRowAtIndexPath:indexPath animated:YES scrollPosition:UITableViewScrollPositionNone];
    [self tableView:self.tableView didSelectRowAtIndexPath:indexPath];
    self.lastSelectedIndex = 0;
    
    if (getEntitlementValue(@"get-task-allow")) {
        [self displayProgress:localize(@"login.jit.checking", nil)];
        if (isJITEnabled(false)) {
            [self displayProgress:localize(@"login.jit.enabled", nil)];
            [self displayProgress:nil];
        } else if (@available(iOS 17.0, *)) {
            // enabling JIT for 17.0+ is done when we actually launch the game
        } else {
            [self enableJITWithAltKit];
        }
    } else if (!NSProcessInfo.processInfo.macCatalystApp && !getenv("SIMULATOR_DEVICE_NAME")) {
        [self displayProgress:localize(@"login.jit.fail", nil)];
        [self displayProgress:nil];
        UIAlertController* alert = [UIAlertController alertControllerWithTitle:localize(@"login.jit.fail.title", nil)
            message:localize(@"login.jit.fail.description_unsupported", nil)
            preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction* okAction = [UIAlertAction actionWithTitle:localize(@"OK", nil) style:UIAlertActionStyleDefault handler:^(id action){
            exit(-1);
        }];
        [alert addAction:okAction];
        [self presentViewController:alert animated:YES completion:nil];
    }
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    // Quick settings may have been changed from the full Settings screen
    [self.tableView reloadSections:[NSIndexSet indexSetWithIndex:1] withRowAnimation:UITableViewRowAnimationNone];
    [self restoreHighlightedSelection];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self announceUpdateIfNeeded];
}

// One-time popup the first time a new build is opened.
- (void)announceUpdateIfNeeded {
    NSString *build = [NSString stringWithFormat:@"%@ (%s)", AmethystBuildVersion(), CONFIG_COMMIT];
    NSString *key = @"amethyst.lastSeenBuild";
    NSString *lastSeen = [NSUserDefaults.standardUserDefaults stringForKey:key];
    if ([build isEqualToString:lastSeen] || self.presentedViewController) return;
    [NSUserDefaults.standardUserDefaults setObject:build forKey:key];
    UIAlertController *alert = [UIAlertController
        alertControllerWithTitle:[NSString stringWithFormat:@"Updated to build %@", AmethystBuildVersion()]
        message:[NSString stringWithFormat:@"Commit %s on %s%@", CONFIG_COMMIT, CONFIG_BRANCH,
            lastSeen ? [NSString stringWithFormat:@"\nPrevious: %@", lastSeen] : @""]
        preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:localize(@"OK", nil) style:UIAlertActionStyleDefault handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (UIBarButtonItem *)drawAccountButton {
    if (!self.accountBtnItem) {
        self.accountButton = [UIButton buttonWithType:UIButtonTypeCustom];
        [self.accountButton addTarget:self action:@selector(selectAccount:) forControlEvents:UIControlEventPrimaryActionTriggered];
        self.accountButton.contentHorizontalAlignment = UIControlContentHorizontalAlignmentLeft;

        self.accountButton.titleEdgeInsets = UIEdgeInsetsMake(0, 4, 0, -4);
        self.accountButton.imageView.contentMode = UIViewContentModeScaleAspectFit;
        self.accountButton.titleLabel.lineBreakMode = NSLineBreakByWordWrapping;
        self.accountBtnItem = [[UIBarButtonItem alloc] initWithCustomView:self.accountButton];
    }

    [self updateAccountInfo];
    
    return self.accountBtnItem;
}

- (void)restoreHighlightedSelection {
    // Restore the selected row when the view appears again
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:self.lastSelectedIndex inSection:0];
    [self.tableView selectRowAtIndexPath:indexPath animated:NO scrollPosition:UITableViewScrollPositionNone];
}

#pragma mark - Layout

// Section 0: places (Home, News, Settings) -- the first three options.
// Section 1: quick settings, changed in place.
// Section 2: tools -- every remaining option.
#define kPlacesCount 3
#define kSectionPlaces 0
#define kSectionQuick 1
#define kSectionTools 2

typedef NS_ENUM(NSInteger, AMQuickRow) {
    AMQuickRenderer,
    AMQuickMemory,
    AMQuickResolution,
    AMQuickModernButtons,
    AMQuickButtonScale,
    AMQuickCount
};

- (UIView *)buildHeaderView {
    UIView *header = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 0, 96)];
    UIImageView *logo = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"AppLogo-Vector"]];
    logo.contentMode = UIViewContentModeScaleAspectFill;
    logo.layer.magnificationFilter = kCAFilterNearest;
    logo.translatesAutoresizingMaskIntoConstraints = NO;
    logo.clipsToBounds = YES;

    UILabel *name = [UILabel new];
    name.text = @"Emerald";
    name.font = AMThemeRoundedFont(30, UIFontWeightHeavy);
    name.textColor = UIColor.whiteColor;
    name.adjustsFontSizeToFitWidth = YES;

    UILabel *tagline = [UILabel new];
    tagline.text = @"Minecraft: Java Edition";
    tagline.font = AMThemeRoundedFont(13, UIFontWeightSemibold);
    tagline.textColor = [AMThemeAccent() colorWithAlphaComponent:0.9];

    UIStackView *texts = [[UIStackView alloc] initWithArrangedSubviews:@[name, tagline]];
    texts.axis = UILayoutConstraintAxisVertical;
    texts.spacing = 0;
    UIStackView *row = [[UIStackView alloc] initWithArrangedSubviews:@[logo, texts]];
    row.axis = UILayoutConstraintAxisHorizontal;
    row.alignment = UIStackViewAlignmentCenter;
    row.spacing = 12;
    row.translatesAutoresizingMaskIntoConstraints = NO;
    [header addSubview:row];
    [NSLayoutConstraint activateConstraints:@[
        [logo.widthAnchor constraintEqualToConstant:64],
        [logo.heightAnchor constraintEqualToConstant:64],
        [row.leadingAnchor constraintEqualToAnchor:header.leadingAnchor constant:20],
        [row.trailingAnchor constraintLessThanOrEqualToAnchor:header.trailingAnchor constant:-16],
        [row.centerYAnchor constraintEqualToAnchor:header.centerYAnchor]
    ]];
    return header;
}

- (UIImage *)tileForSymbol:(NSString *)symbol {
    UIImage *glyph = [UIImage systemImageNamed:symbol];
    if (!glyph) return nil;
    UIImageSymbolConfiguration *config = [UIImageSymbolConfiguration configurationWithPointSize:15 weight:UIImageSymbolWeightBold];
    glyph = [[glyph imageByApplyingSymbolConfiguration:config] imageWithTintColor:AMThemeAccent() renderingMode:UIImageRenderingModeAlwaysOriginal];
    CGSize size = CGSizeMake(32, 32);
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size];
    return [renderer imageWithActions:^(UIGraphicsImageRendererContext *ctx) {
        UIBezierPath *tile = [UIBezierPath bezierPathWithRoundedRect:CGRectMake(0, 0, size.width, size.height) cornerRadius:8];
        [[AMThemeAccent() colorWithAlphaComponent:0.16] setFill];
        [tile fill];
        CGSize g = glyph.size;
        [glyph drawInRect:CGRectMake((size.width - g.width) / 2, (size.height - g.height) / 2, g.width, g.height)];
    }];
}

- (NSInteger)optionIndexForIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == kSectionPlaces) return indexPath.row;
    if (indexPath.section == kSectionTools) return kPlacesCount + indexPath.row;
    return -1;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 3;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    switch (section) {
        case kSectionQuick: return @"Quick Settings";
        case kSectionTools: return @"Tools";
        default: return nil;
    }
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    switch (section) {
        case kSectionPlaces: return MIN(kPlacesCount, self.options.count);
        case kSectionQuick: return realUIIdiom == UIUserInterfaceIdiomTV ? AMQuickCount - 2 : AMQuickCount;
        default: return MAX(0, (NSInteger)self.options.count - kPlacesCount);
    }
}

#pragma mark - Quick settings

- (NSString *)rendererDisplayName {
    NSString *current = getPrefObject(@"video.renderer");
    NSArray *keys = getRendererKeys(NO);
    NSArray *names = getRendererNames(NO);
    NSUInteger i = [keys indexOfObject:current];
    return (i != NSNotFound && i < names.count) ? names[i] : current;
}

- (NSString *)memoryDisplayValue {
    if (getPrefBool(@"java.auto_ram")) return @"Auto";
    return [NSString stringWithFormat:@"%ld MB", (long)getPrefInt(@"java.allocated_memory")];
}

- (void)configureQuickCell:(UITableViewCell *)cell row:(NSInteger)row {
    NSString *title, *value, *icon;
    cell.accessoryView = nil;
    cell.accessoryType = UITableViewCellAccessoryNone;
    switch (row) {
        case AMQuickRenderer:
            title = @"Renderer"; icon = @"cpu"; value = [self rendererDisplayName]; break;
        case AMQuickMemory:
            title = @"Memory"; icon = @"memorychip"; value = [self memoryDisplayValue]; break;
        case AMQuickResolution:
            title = @"Resolution"; icon = @"aspectratio";
            value = [NSString stringWithFormat:@"%ld%%", (long)getPrefInt(@"video.resolution")]; break;
        case AMQuickModernButtons: {
            title = @"Bedrock Buttons"; icon = @"circle.grid.cross.fill";
            UISwitch *toggle = [UISwitch new];
            toggle.on = getPrefBool(@"control.modern_buttons");
            [toggle addTarget:self action:@selector(toggleModernButtons:) forControlEvents:UIControlEventValueChanged];
            cell.accessoryView = toggle;
            break;
        }
        case AMQuickButtonScale:
            title = @"Button Size"; icon = @"plusminus.circle.fill";
            value = [NSString stringWithFormat:@"%ld%%", (long)getPrefInt(@"control.button_scale")]; break;
    }
    cell.textLabel.text = title;
    cell.detailTextLabel.text = value;
    cell.detailTextLabel.textColor = [UIColor colorWithWhite:1.0 alpha:0.55];
    cell.detailTextLabel.font = AMThemeRoundedFont(15, UIFontWeightMedium);
    cell.imageView.image = [self tileForSymbol:icon];
    if (value) cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
}

- (void)toggleModernButtons:(UISwitch *)sender {
    setPrefBool(@"control.modern_buttons", sender.on);
}

- (void)presentChoices:(NSString *)title options:(NSArray<NSString *> *)labels from:(NSIndexPath *)indexPath handler:(void (^)(NSInteger index))handler {
    UIAlertController *sheet = [UIAlertController alertControllerWithTitle:title message:nil preferredStyle:UIAlertControllerStyleActionSheet];
    [labels enumerateObjectsUsingBlock:^(NSString *label, NSUInteger i, BOOL *stop) {
        [sheet addAction:[UIAlertAction actionWithTitle:label style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
            handler(i);
            [self.tableView reloadRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationNone];
        }]];
    }];
    [sheet addAction:[UIAlertAction actionWithTitle:localize(@"Cancel", nil) style:UIAlertActionStyleCancel handler:nil]];
    UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:indexPath];
    sheet.popoverPresentationController.sourceView = cell ?: self.view;
    sheet.popoverPresentationController.sourceRect = cell ? cell.bounds : self.view.bounds;
    [self presentViewController:sheet animated:YES completion:nil];
}

- (void)selectQuickRow:(NSIndexPath *)indexPath {
    switch (indexPath.row) {
        case AMQuickRenderer: {
            NSArray *keys = getRendererKeys(NO);
            [self presentChoices:@"Renderer" options:getRendererNames(NO) from:indexPath handler:^(NSInteger i) {
                setPrefObject(@"video.renderer", keys[i]);
            }];
            break;
        }
        case AMQuickMemory: {
            NSArray<NSNumber *> *sizes = @[@1024, @1536, @2048, @3072, @4096];
            NSMutableArray *labels = [NSMutableArray arrayWithObject:@"Auto"];
            for (NSNumber *mb in sizes) [labels addObject:[NSString stringWithFormat:@"%@ MB", mb]];
            [self presentChoices:@"Memory" options:labels from:indexPath handler:^(NSInteger i) {
                setPrefBool(@"java.auto_ram", i == 0);
                if (i > 0) setPrefInt(@"java.allocated_memory", sizes[i - 1].integerValue);
            }];
            break;
        }
        case AMQuickResolution: {
            NSArray<NSNumber *> *values = @[@50, @67, @75, @85, @100];
            NSMutableArray *labels = [NSMutableArray new];
            for (NSNumber *v in values) [labels addObject:[NSString stringWithFormat:@"%@%%", v]];
            [self presentChoices:@"Resolution" options:labels from:indexPath handler:^(NSInteger i) {
                setPrefInt(@"video.resolution", values[i].integerValue);
            }];
            break;
        }
        case AMQuickButtonScale: {
            NSArray<NSNumber *> *values = @[@75, @90, @100, @115, @130, @150];
            NSMutableArray *labels = [NSMutableArray new];
            for (NSNumber *v in values) [labels addObject:[NSString stringWithFormat:@"%@%%", v]];
            [self presentChoices:@"Button Size" options:labels from:indexPath handler:^(NSInteger i) {
                setPrefInt(@"control.button_scale", values[i].integerValue);
            }];
            break;
        }
        default:
            break;
    }
}

#pragma mark - Table

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    BOOL quick = indexPath.section == kSectionQuick;
    NSString *identifier = quick ? @"quick" : @"cell";
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:identifier];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:(quick ? UITableViewCellStyleValue1 : UITableViewCellStyleDefault) reuseIdentifier:identifier];
        UIView *selected = [UIView new];
        selected.backgroundColor = [AMThemeAccent() colorWithAlphaComponent:0.22];
        cell.selectedBackgroundView = selected;
    }
    cell.backgroundColor = AMThemeSurface();
    cell.textLabel.font = AMThemeRoundedFont(17, UIFontWeightSemibold);
    cell.textLabel.textColor = UIColor.whiteColor;
    cell.textLabel.adjustsFontSizeToFitWidth = YES;
    cell.textLabel.minimumScaleFactor = 0.75;

    if (quick) {
        [self configureQuickCell:cell row:indexPath.row];
        return cell;
    }

    LauncherMenuCustomItem *item = self.options[[self optionIndexForIndexPath:indexPath]];
    cell.textLabel.text = item.title;
    cell.imageView.image = [self tileForSymbol:item.imageName];
    if (!cell.imageView.image && item.imageName.length > 0) {
        cell.imageView.layer.magnificationFilter = kCAFilterNearest;
        cell.imageView.image = [[UIImage imageNamed:item.imageName] _imageWithSize:CGSizeMake(32, 32)];
    }
    cell.accessoryType = UITableViewCellAccessoryNone;
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.section == kSectionQuick) {
        [self restoreHighlightedSelection];
        [self selectQuickRow:indexPath];
        return;
    }

    NSInteger optionIndex = [self optionIndexForIndexPath:indexPath];
    LauncherMenuCustomItem *selected = self.options[optionIndex];
    
    if (selected.action != nil) {
        [self restoreHighlightedSelection];
        ((LauncherMenuCustomItem *)selected).action();
    } else {
        if(self.isInitialVc) {
            self.isInitialVc = NO;
        } else {
            self.options[self.lastSelectedIndex].vcArray = contentNavigationController.viewControllers;
            [contentNavigationController setViewControllers:selected.vcArray animated:NO];
            self.lastSelectedIndex = optionIndex;
        }
        selected.vcArray[0].navigationItem.rightBarButtonItem = self.accountBtnItem;
        selected.vcArray[0].navigationItem.leftBarButtonItem = self.splitViewController.displayModeButtonItem;
        selected.vcArray[0].navigationItem.leftItemsSupplementBackButton = true;
    }
}

- (void)selectAccount:(UIButton *)sender {
    AccountListViewController *vc = [[AccountListViewController alloc] init];
    vc.whenDelete = ^void(NSString* name) {
        if ([name isEqualToString:getPrefObject(@"internal.selected_account")]) {
            BaseAuthenticator.current = nil;
            setPrefObject(@"internal.selected_account", @"");
            [self updateAccountInfo];
        }
    };
    vc.whenItemSelected = ^void() {
        setPrefObject(@"internal.selected_account", BaseAuthenticator.current.authData[@"username"]);
        [self updateAccountInfo];
        if (sender != self.accountButton) {
            // Called from the play button, so call back to continue
            [sender sendActionsForControlEvents:UIControlEventPrimaryActionTriggered];
        }
    };
    vc.modalPresentationStyle = UIModalPresentationPopover;
    vc.preferredContentSize = CGSizeMake(350, 250);

    UIPopoverPresentationController *popoverController = vc.popoverPresentationController;
    popoverController.sourceView = sender;
    popoverController.sourceRect = sender.bounds;
    popoverController.permittedArrowDirections = UIPopoverArrowDirectionAny;
    popoverController.delegate = vc;
    [self presentViewController:vc animated:YES completion:nil];
}

- (void)updateAccountInfo {
    NSDictionary *selected = BaseAuthenticator.current.authData;
    CGSize size = CGSizeMake(contentNavigationController.view.frame.size.width, contentNavigationController.view.frame.size.height);
    
    if (selected == nil) {
        if((size.width / 3) > 200) {
            [self.accountButton setAttributedTitle:[[NSAttributedString alloc] initWithString:localize(@"login.option.select", nil)] forState:UIControlStateNormal];
        } else {
            [self.accountButton setAttributedTitle:(NSAttributedString *)@"" forState:UIControlStateNormal];
        }
        [self.accountButton setImage:[UIImage imageNamed:@"DefaultAccount"] forState:UIControlStateNormal];
        [self.accountButton sizeToFit];
        return;
    }

    // Remove the prefix "Demo." if there is
    BOOL isDemo = [selected[@"username"] hasPrefix:@"Demo."];
    NSMutableAttributedString *title = [[NSMutableAttributedString alloc] initWithString:[selected[@"username"] substringFromIndex:(isDemo?5:0)]];

    // Check if we're switching between demo and full mode
    BOOL shouldUpdateProfiles = (getenv("DEMO_LOCK")!=NULL) != isDemo;

    // Reset states
    unsetenv("DEMO_LOCK");
    setenv("POJAV_GAME_DIR", [NSString stringWithFormat:@"%s/Library/Application Support/minecraft", getenv("POJAV_HOME")].UTF8String, 1);

    id subtitle;
    if (isDemo) {
        subtitle = localize(@"login.option.demo", nil);
        setenv("DEMO_LOCK", "1", 1);
        setenv("POJAV_GAME_DIR", [NSString stringWithFormat:@"%s/.demo", getenv("POJAV_HOME")].UTF8String, 1);
    } else if (selected[@"xboxGamertag"] == nil) {
        subtitle = localize(@"login.option.local", nil);
    } else {
        // Display the Xbox gamertag for online accounts
        subtitle = selected[@"xboxGamertag"];
    }

    subtitle = [[NSAttributedString alloc] initWithString:subtitle attributes:@{NSFontAttributeName: [UIFont systemFontOfSize:12]}];
    [title appendAttributedString:[[NSAttributedString alloc] initWithString:@"\n" attributes:nil]];
    [title appendAttributedString:subtitle];
    
    if((size.width / 3) > 200) {
        [self.accountButton setAttributedTitle:title forState:UIControlStateNormal];
    } else {
        [self.accountButton setAttributedTitle:(NSAttributedString *)@"" forState:UIControlStateNormal];
    }
    
    // TODO: Add caching mechanism for profile pictures
    NSURL *url = [NSURL URLWithString:[selected[@"profilePicURL"] stringByReplacingOccurrencesOfString:@"\\/" withString:@"/"]];
    UIImage *placeholder = [UIImage imageNamed:@"DefaultAccount"];
    [self.accountButton setImageForState:UIControlStateNormal withURL:url placeholderImage:placeholder];
    [self.accountButton.imageView setImageWithURL:url placeholderImage:placeholder];
    [self.accountButton sizeToFit];

    // Update profiles and local version list if needed
    if (shouldUpdateProfiles) {
        [contentNavigationController fetchLocalVersionList];
        [contentNavigationController performSelector:@selector(reloadProfileList)];
    }

    // Update tableView whenever we have
    UITableViewController *tableVC = contentNavigationController.viewControllers.lastObject;
    if ([tableVC isKindOfClass:UITableViewController.class]) {
        [tableVC.tableView reloadData];
    }
}

- (void)displayProgress:(NSString *)status {
    if (status == nil) {
        [(UIActivityIndicatorView *)self.toolbarItems[0].customView stopAnimating];
    } else {
        self.toolbarItems[1].title = status;
    }
}

- (void)enableJITWithAltKit {
    [ALTServerManager.sharedManager startDiscovering];
    [ALTServerManager.sharedManager autoconnectWithCompletionHandler:^(ALTServerConnection *connection, NSError *error) {
        if (error) {
            NSLog(@"[AltKit] Could not auto-connect to server. %@", error.localizedRecoverySuggestion);
            [self displayProgress:localize(@"login.jit.fail", nil)];
            [self displayProgress:nil];
        }
        [connection enableUnsignedCodeExecutionWithCompletionHandler:^(BOOL success, NSError *error) {
            if (success) {
                NSLog(@"[AltKit] Successfully enabled JIT compilation!");
                [ALTServerManager.sharedManager stopDiscovering];
                [self displayProgress:localize(@"login.jit.enabled", nil)];
                [self displayProgress:nil];
            } else {
                NSLog(@"[AltKit] Error enabling JIT: %@", error.localizedRecoverySuggestion);
                [self displayProgress:localize(@"login.jit.fail", nil)];
                [self displayProgress:nil];
            }
            [connection disconnect];
        }];
    }];
}

@end
