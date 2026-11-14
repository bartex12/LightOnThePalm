package com.bartex.adresslentawifi.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.bartex.adresslentawifi.R
import com.bartex.adresslentawifi.databinding.FragmentHelpBinding
import com.bartex.adresslentawifi.databinding.ItemHelpSectionBinding

/**
 * Справка (пункт "Справка" в шторке): список разделов, по нажатию раздел раскрывается.
 * Чтобы добавить раздел, достаточно добавить две строки в strings.xml и пару в SECTIONS.
 * Тексты разделов написаны в HTML (цвет, жирный шрифт, переносы строк).
 */
class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    private val opened = BooleanArray(SECTIONS.size) //какие разделы раскрыты

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getBooleanArray(KEY_OPENED)?.let {
            if (it.size == opened.size) it.copyInto(opened)
        }
        SECTIONS.forEachIndexed { index, (title, text) ->
            val item = ItemHelpSectionBinding.inflate(layoutInflater, binding.helpContainer, false)
            item.tvHelpTitle.setText(title)
            item.tvHelpText.text = HtmlCompat.fromHtml(getString(text), HtmlCompat.FROM_HTML_MODE_LEGACY)
            showSection(item, opened[index])
            item.root.setOnClickListener {
                opened[index] = !opened[index]
                showSection(item, opened[index])
            }
            binding.helpContainer.addView(item.root)
        }
    }

    private fun showSection(item: ItemHelpSectionBinding, open: Boolean) {
        item.tvHelpText.visibility = if (open) View.VISIBLE else View.GONE
        item.tvHelpArrow.text = if (open) "▴" else "▾"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBooleanArray(KEY_OPENED, opened)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_OPENED = "help_opened"

        //заголовок и текст каждого раздела, в порядке показа
        private val SECTIONS: List<Pair<Int, Int>> = listOf(
            R.string.helpS1Title to R.string.helpS1Text,
            R.string.helpS2Title to R.string.helpS2Text,
            R.string.helpS3Title to R.string.helpS3Text,
            R.string.helpS4Title to R.string.helpS4Text,
            R.string.helpS5Title to R.string.helpS5Text,
            R.string.helpS6Title to R.string.helpS6Text,
            R.string.helpS7Title to R.string.helpS7Text,
            R.string.helpS8Title to R.string.helpS8Text,
            R.string.helpS9Title to R.string.helpS9Text,
        )
    }
}
