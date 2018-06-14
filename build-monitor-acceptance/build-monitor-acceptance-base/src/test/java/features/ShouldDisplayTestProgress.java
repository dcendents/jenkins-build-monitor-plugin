package features;

import com.smartcodeltd.jenkinsci.plugins.build_monitor.questions.ProjectWidget;
import com.smartcodeltd.jenkinsci.plugins.build_monitor.tasks.CreateABuildMonitorView;
import com.smartcodeltd.jenkinsci.plugins.build_monitor.tasks.configuration.DisplayAllProjects;
import com.smartcodeltd.jenkinsci.plugins.build_monitor.tasks.configuration.DisplayJunitRealtimeProgress;

import environment.JenkinsSandbox;
import net.serenitybdd.integration.jenkins.JenkinsInstance;
import net.serenitybdd.integration.jenkins.environment.rules.InstallPlugins;
import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.jenkins.HaveAPipelineProjectCreated;
import net.serenitybdd.screenplay.jenkins.tasks.ScheduleABuild;
import net.serenitybdd.screenplay.jenkins.tasks.configuration.build_steps.SetPipelineDefinition;
import net.serenitybdd.screenplayx.actions.Navigate;
import net.thucydides.core.annotations.Managed;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import static net.serenitybdd.screenplay.GivenWhenThen.*;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isCurrentlyVisible;
import static org.hamcrest.Matchers.containsString;

@RunWith(SerenityRunner.class)
public class ShouldDisplayTestProgress {

    Actor richard = Actor.named("Richard");

    @Managed public WebDriver browser;

    @Rule
    public JenkinsInstance jenkins = JenkinsSandbox.configure().afterStart(
            InstallPlugins.fromUpdateCenter("workflow-aggregator", "junit-realtime-test-reporter")
    ).create();

    @Before
    public void actorCanBrowseTheWeb() {
        richard.can(BrowseTheWeb.with(browser));
    }

    @Test
    public void displaying_current_pipeline_stage() throws Exception {
        givenThat(richard).wasAbleTo(
                Navigate.to(jenkins.url()),
                HaveAPipelineProjectCreated.called("My Pipeline").andConfiguredTo(
                        SetPipelineDefinition.asFollows("node {\r\n" + 
                        		"	parallel firstBranch: {\r\n" + 
                        		"		stage('stage1') {\r\n" + 
                        		"			realtimeJUnit('a*.xml') {\r\n" + 
                        		"				writeFile text: '''<testsuite name='aa'><testcase name='aa1'/><testcase name='aa2'/></testsuite>''', file: 'aa.xml'\r\n" + 
                        		"				if (currentBuild.number > 1) {\r\n" + 
                        		"					sleep 60\r\n" + 
                        		"				}\r\n" + 
                        		"				writeFile text: '''<testsuite name='ab'><testcase name='ab1'/><testcase name='ab2'/></testsuite>''', file: 'ab.xml'\r\n" + 
                        		"			}\r\n" + 
                        		"		}\r\n" + 
                        		"	}, secondBranch: {\r\n" + 
                        		"		stage('stage2') {\r\n" + 
                        		"			realtimeJUnit('b*.xml') {\r\n" + 
                        		"				writeFile text: '''<testsuite name='ba'><testcase name='ba1'/><testcase name='ba2'><error>b2 failed</error></testcase></testsuite>''', file: 'ba.xml'\r\n" + 
                        		"				if (currentBuild.number > 1) {\r\n" + 
                        		"					sleep 60\r\n" + 
                        		"				}\r\n" + 
                        		"				writeFile text: '''<testsuite name='bb'><testcase name='bb1'/><testcase name='bb2'><error>b2 failed</error></testcase></testsuite>''', file: 'bb.xml'\r\n" + 
                        		"			}\r\n" + 
                        		"		}\r\n" + 
                        		"	},\r\n" + 
                        		"	failFast: true\r\n" + 
                        		"}")
                ),

                ScheduleABuild.of("My Pipeline"),
                ScheduleABuild.of("My Pipeline")
        );

        when(richard).attemptsTo(CreateABuildMonitorView.called("Build Monitor").andConfigureItTo(
                DisplayAllProjects.usingARegularExpression(),
                DisplayJunitRealtimeProgress.bars()
        ));
        

        then(richard).should(seeThat(ProjectWidget.of("My Pipeline").pipelineStages(),
                containsString("[Compile]")
        ));
        then(richard).should(seeThat(ProjectWidget.of("My Pipeline").testProgressBars(),
                isCurrentlyVisible()
        ));
    }

}
