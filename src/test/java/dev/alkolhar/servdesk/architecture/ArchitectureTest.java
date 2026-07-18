package dev.alkolhar.servdesk.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Encodes the layering rules described in CLAUDE.md so a future change can't
 * quietly drift away from them. Kept small and high-confidence on purpose — see
 * Phase 3 notes for a rule that was considered (controllers must never
 * reference an {@code @Entity} type) and dropped because ArchUnit's
 * bytecode-level dependency resolution flags the transient
 * {@code assembler.toModel(
 * queryService.findById(id))} call chain as a "dependency", which would make
 * the rule fail on exactly the pattern CLAUDE.md recommends rather than the one
 * it forbids.
 */
@AnalyzeClasses(packages = "dev.alkolhar.servdesk", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule feature_packages_are_free_of_cycles = slices().matching("dev.alkolhar.servdesk.(*)..")
			.should().beFreeOfCycles();

	@ArchTest
	static final ArchRule controllers_do_not_access_repositories_directly = noClasses().that()
			.haveSimpleNameEndingWith("Controller").should().dependOnClassesThat()
			.haveSimpleNameEndingWith("Repository");

	@ArchTest
	static final ArchRule command_and_query_services_stay_free_of_web_layer_types = noClasses().that()
			.haveSimpleNameEndingWith("CommandService").or().haveSimpleNameEndingWith("QueryService").should()
			.dependOnClassesThat().resideInAPackage("org.springframework.web..");
}
