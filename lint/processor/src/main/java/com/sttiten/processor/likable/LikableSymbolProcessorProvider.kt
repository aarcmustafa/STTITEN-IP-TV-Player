
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            logger = environment.logger,
            codeGenerator = environment.codeGenerator
        )
    }
}
