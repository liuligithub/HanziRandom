import XCTest

final class SmokeUITests: XCTestCase {
    private func launchFreshApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--uitest-reset"]
        app.launch()
        XCTAssertTrue(app.tabBars.buttons["学习"].waitForExistence(timeout: 15))
        return app
    }

    /// Pulls the position out of the "第 N / 共 M" progress label.
    private func progressPosition(_ label: String) -> Int? {
        guard let range = label.range(of: #"第 \d+ /"#, options: .regularExpression) else { return nil }
        return Int(label[range].filter(\.isNumber))
    }

    func test_learningNavigationAndMistakeBook() {
        let app = launchFreshApp()

        let progress = app.staticTexts["progressLabel"]
        XCTAssertTrue(progress.waitForExistence(timeout: 5))
        XCTAssertEqual(progressPosition(progress.label), 1, "fresh round should start at 1: \(progress.label)")

        let next = app.buttons["nextButton"]
        XCTAssertTrue(next.isHittable)
        next.tap()
        XCTAssertEqual(progressPosition(progress.label), 2)

        let prev = app.buttons["previousButton"]
        XCTAssertTrue(prev.isEnabled)
        prev.tap()
        XCTAssertEqual(progressPosition(progress.label), 1)

        let speak = app.buttons["speakButton"]
        XCTAssertTrue(speak.isHittable)
        speak.tap()

        let add = app.buttons["addMistakeButton"]
        XCTAssertTrue(add.isHittable)
        add.tap()

        app.tabBars.buttons["错题本"].tap()
        XCTAssertTrue(app.navigationBars["错题本"].waitForExistence(timeout: 5))
        let mistakeRow = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH '错 '")).firstMatch
        XCTAssertTrue(mistakeRow.waitForExistence(timeout: 5), "mistake entry not shown")

        mistakeRow.tap()
        XCTAssertTrue(app.buttons["detailSpeakButton"].waitForExistence(timeout: 5), "detail speak button missing")
        XCTAssertTrue(app.buttons["detailCopyButton"].exists)
        app.buttons["关闭"].tap()

        let review = app.buttons["reviewMistakesButton"]
        XCTAssertTrue(review.waitForExistence(timeout: 5))
        review.tap()
        XCTAssertTrue(app.staticTexts["错题复习"].waitForExistence(timeout: 5))

        let delete = app.buttons["deleteMistakeButton"]
        XCTAssertTrue(delete.waitForExistence(timeout: 5))
        XCTAssertTrue(delete.isHittable, "删除错题 must fit in the review layout")
        app.buttons["关闭"].tap()
    }

    func test_clearMistakesRequiresConfirmation() {
        let app = launchFreshApp()

        let add = app.buttons["addMistakeButton"]
        XCTAssertTrue(add.waitForExistence(timeout: 5))
        add.tap()

        app.tabBars.buttons["错题本"].tap()
        let clear = app.buttons["clearMistakesButton"]
        XCTAssertTrue(clear.waitForExistence(timeout: 5))
        clear.tap()

        XCTAssertTrue(app.buttons["取消"].waitForExistence(timeout: 3))
        app.buttons["取消"].tap()
        XCTAssertFalse(app.buttons["清空"].waitForExistence(timeout: 2), "dialog should close on 取消")
        XCTAssertTrue(app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH '错 '")).firstMatch.exists,
                      "取消 must keep the mistake entries")

        clear.tap()
        XCTAssertTrue(app.buttons["清空"].waitForExistence(timeout: 3))
        app.buttons["清空"].tap()
        XCTAssertTrue(app.staticTexts["还没有错题"].waitForExistence(timeout: 5))
    }
}
